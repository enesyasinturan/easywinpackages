package com.softeyt.easywinpackages.service;

import com.google.inject.Singleton;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Singleton
public class CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(CommandExecutor.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    
    private static final Charset CMD_CHARSET = Charset.forName("IBM437");

    
    private volatile String wingetRealPath;

    public CommandExecutor() {
        refreshWingetPath();
    }

    
    public Task<String> execute(List<String> command) {
        return execute(command, DEFAULT_TIMEOUT_SECONDS);
    }

    
    public Task<String> execute(List<String> command, int timeoutSeconds) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                List<String> resolved = resolveCommand(command);
                log.info("Executing: {}", String.join(" ", command));

                ProcessBuilder pb = new ProcessBuilder(resolved);
                pb.redirectErrorStream(true);
                pb.environment().putAll(buildEnv());

                Process process = pb.start();
                String output;
                
                Charset charset = isWingetDirect(resolved) ? StandardCharsets.UTF_8 : CMD_CHARSET;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), charset))) {
                    output = reader.lines().collect(Collectors.joining("\n"));
                }

                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    log.warn("Command timed out after {}s: {}", timeoutSeconds, String.join(" ", command));
                    throw new IOException("Command timed out after " + timeoutSeconds + "s");
                }

                int exitCode = process.exitValue();
                log.debug("Exit code {}: {}", exitCode, String.join(" ", command));
                if (exitCode != 0) {
                    String message = "Command failed with exit code " + exitCode + ": " + abbreviateOutput(output);
                    log.warn("{} | command={}", message, String.join(" ", command));
                    throw new IOException(message);
                }
                return output;
            }
        };
    }

    
    public Task<String> powershell(String psCommand) {
        return powershell(psCommand, DEFAULT_TIMEOUT_SECONDS);
    }

    
    public Task<String> powershell(String psCommand, int timeoutSeconds) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                List<String> cmd = List.of(
                        "powershell.exe", "-NoProfile", "-NonInteractive",
                        "-OutputFormat", "Text",
                        "-Command", "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; " + psCommand);
                log.info("PowerShell: {}", psCommand);
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                pb.environment().putAll(buildEnv());
                Process process = pb.start();
                String output;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    output = reader.lines().collect(Collectors.joining("\n"));
                }
                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new IOException("PowerShell timed out after " + timeoutSeconds + "s");
                }
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String message = "PowerShell failed with exit code " + exitCode + ": " + abbreviateOutput(output);
                    log.warn("{} | command={}", message, psCommand);
                    throw new IOException(message);
                }
                return output;
            }
        };
    }

    
    public boolean isCommandAvailable(String name) {
        if ("winget".equalsIgnoreCase(name)) {
            return refreshWingetPath() != null;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "where", name);
            pb.redirectErrorStream(true);
            pb.environment().putAll(buildEnv());
            Process p = pb.start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Availability check failed for {}: {}", name, e.getMessage());
            return false;
        }
    }

    
    public boolean isScoopAvailable() {
        Path scoopShim = resolveScoopShimPath();
        if (scoopShim != null && testCommandBinary(List.of(scoopShim.toString(), "--version"), StandardCharsets.UTF_8)) {
            return true;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive",
                    "-Command", "Get-Command scoop -ErrorAction SilentlyContinue");
            pb.redirectErrorStream(true);
            pb.environment().putAll(buildEnv());
            Process p = pb.start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Scoop availability check failed: {}", e.getMessage());
            return false;
        }
    }

    

    
    private static String resolveWingetPath() {
        
        String userAppData = System.getenv("LOCALAPPDATA");
        if (userAppData != null) {
            Path alias = Path.of(userAppData, "Microsoft", "WindowsApps", "winget.exe");
            if (Files.exists(alias)) {
                
                if (testWingetBinary(alias.toString())) {
                    return alias.toString();
                }
                log.debug("App Execution Alias found but not executable: {}", alias);
            }
        }

        String appxPackagePath = tryResolveWingetFromAppxPackage();
        if (appxPackagePath != null && testWingetBinary(appxPackagePath)) {
            return appxPackagePath;
        }

        
        Path windowsApps = Path.of("C:\\Program Files\\WindowsApps");
        if (Files.isDirectory(windowsApps)) {
            try (var stream = Files.walk(windowsApps, 2)) {
                Path found = stream
                        .filter(p -> p.getFileName().toString().equals("winget.exe"))
                        .filter(p -> p.getParent().getFileName().toString()
                                .startsWith("Microsoft.DesktopAppInstaller"))
                        .findFirst().orElse(null);
                if (found != null && testWingetBinary(found.toString())) {
                    return found.toString();
                }
            } catch (Exception e) {
                log.debug("Could not walk WindowsApps: {}", e.getMessage());
            }
        }

        
        return tryWingetViaCmdPath();
    }

    
    public synchronized String refreshWingetPath() {
        String resolved = resolveWingetPath();
        if (!java.util.Objects.equals(this.wingetRealPath, resolved)) {
            this.wingetRealPath = resolved;
            if (resolved != null) {
                log.info("Winget binary resolved: {}", resolved);
            } else {
                log.warn("Winget binary could not be resolved; winget commands may fail");
            }
        }
        return this.wingetRealPath;
    }

    
    private static boolean testWingetBinary(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            pb.redirectErrorStream(true);
            pb.environment().putAll(buildEnv());
            Process p = pb.start();
            String out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                out = r.lines().collect(Collectors.joining());
            }
            p.waitFor(5, TimeUnit.SECONDS);
            boolean ok = p.exitValue() == 0 && looksLikeWingetVersion(out);
            log.debug("winget binary test {}: {} → {}", ok ? "OK" : "FAIL", path, out.trim());
            return ok;
        } catch (Exception e) {
            log.debug("winget binary test failed for {}: {}", path, e.getMessage());
            return false;
        }
    }

    
    private static String tryWingetViaCmdPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "winget", "--version");
            pb.redirectErrorStream(true);
            pb.environment().putAll(buildEnv());
            Process p = pb.start();
            String out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                out = r.lines().collect(Collectors.joining());
            }
            p.waitFor(5, TimeUnit.SECONDS);
            if (p.exitValue() == 0 && looksLikeWingetVersion(out)) {
                log.debug("winget available via cmd /c PATH");
                return "CMD_PATH"; 
            }
        } catch (Exception e) {
            log.debug("cmd /c winget test failed: {}", e.getMessage());
        }
        return null;
    }

    private static String tryResolveWingetFromAppxPackage() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive",
                    "-Command",
                    "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; " +
                            "$pkg=Get-AppxPackage -Name 'Microsoft.DesktopAppInstaller' -ErrorAction SilentlyContinue | " +
                            "Sort-Object Version -Descending | Select-Object -First 1; " +
                            "if ($null -ne $pkg -and -not [string]::IsNullOrWhiteSpace($pkg.InstallLocation)) { " +
                            "Join-Path $pkg.InstallLocation 'winget.exe' }");
            pb.redirectErrorStream(true);
            pb.environment().putAll(buildEnv());
            Process p = pb.start();
            String out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                out = r.lines().collect(Collectors.joining("\n")).trim();
            }
            if (!p.waitFor(8, TimeUnit.SECONDS) || p.exitValue() != 0 || out.isBlank()) {
                return null;
            }
            Path candidate = Path.of(out);
            return Files.exists(candidate) ? candidate.toString() : null;
        } catch (Exception e) {
            log.debug("Appx winget location probe failed: {}", e.getMessage());
            return null;
        }
    }

    private static Path resolveScoopShimPath() {
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile == null || userProfile.isBlank()) {
            return null;
        }
        Path shim = Path.of(userProfile, "scoop", "shims", "scoop.cmd");
        return Files.exists(shim) ? shim : null;
    }

    
    private static Map<String, String> buildEnv() {
        Map<String, String> env = new java.util.HashMap<>(System.getenv());
        
        String userPath   = getRegPath("HKCU\\Environment", "PATH");
        String systemPath = getRegPath("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment", "PATH");
        if (userPath != null || systemPath != null) {
            String merged = List.of(
                    userPath   != null ? userPath   : "",
                    systemPath != null ? systemPath : "",
                    env.getOrDefault("PATH", ""))
                    .stream().filter(s -> !s.isBlank())
                    .collect(Collectors.joining(";"));
            env.put("PATH", merged);
        }
        return env;
    }

    
    private static String getRegPath(String key, String value) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", key, "/v", value);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                out = r.lines().collect(Collectors.joining("\n"));
            }
            p.waitFor(3, TimeUnit.SECONDS);
            
            for (String line : out.split("\n")) {
                if (line.contains("REG_") && line.contains(value)) {
                    int idx = line.indexOf("REG_EXPAND_SZ");
                    if (idx < 0) idx = line.indexOf("REG_SZ");
                    if (idx >= 0) {
                        String val = line.substring(idx).replaceFirst("REG_(EXPAND_)?SZ\\s*", "").trim();
                        return expandEnvVars(val);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("reg query failed for {}: {}", key, e.getMessage());
        }
        return null;
    }

    private static String expandEnvVars(String s) {
        if (s == null) return null;
        
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("%([^%]+)%").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String val = System.getenv(m.group(1));
            m.appendReplacement(sb, val != null ? java.util.regex.Matcher.quoteReplacement(val) : m.group(0));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    
    private List<String> resolveCommand(List<String> command) {
        if (command.isEmpty()) return command;
        String first = command.get(0);
        if ("winget".equalsIgnoreCase(first)) {
            String resolvedWingetPath = refreshWingetPath();
            if (resolvedWingetPath == null) {
                List<String> wrapped = new ArrayList<>();
                wrapped.add("cmd");
                wrapped.add("/c");
                wrapped.addAll(command);
                return wrapped;
            }
            if ("CMD_PATH".equals(resolvedWingetPath)) {
                
                List<String> wrapped = new ArrayList<>();
                wrapped.add("cmd"); wrapped.add("/c");
                wrapped.addAll(command);
                return wrapped;
            }
            
            List<String> direct = new ArrayList<>(command);
            direct.set(0, resolvedWingetPath);
            return direct;
        }
        
        List<String> wrapped = new ArrayList<>();
        wrapped.add("cmd"); wrapped.add("/c");
        wrapped.addAll(command);
        return wrapped;
    }

    private boolean isWingetDirect(List<String> resolved) {
        return !resolved.isEmpty() && !"cmd".equals(resolved.get(0));
    }

    private static boolean testCommandBinary(List<String> command, Charset charset) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.environment().putAll(buildEnv());
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                reader.lines().collect(Collectors.joining("\n"));
            }
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("Binary test failed for {}: {}", command, e.getMessage());
            return false;
        }
    }

    private static boolean looksLikeWingetVersion(String output) {
        if (output == null) {
            return false;
        }
        String normalized = output.trim();
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.matches("(?is).*v?\\d+(\\.\\d+)+.*");
    }

    private static String abbreviateOutput(String output) {
        if (output == null || output.isBlank()) {
            return "<no output>";
        }
        String normalized = output.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220) + "...";
    }
}
