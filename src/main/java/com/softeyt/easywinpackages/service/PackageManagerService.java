package com.softeyt.easywinpackages.service;

import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.model.UpdateInfo;
import javafx.concurrent.Task;

import java.util.List;


public interface PackageManagerService {

    
    Task<List<Package>> search(String query);

    
    Task<Void> install(Package pkg);

    
    Task<Void> uninstall(Package pkg);

    
    Task<List<UpdateInfo>> getUpdates();

    
    Task<Void> update(UpdateInfo update);

    
    Task<Void> updateAll();

    
    Task<List<Package>> getInstalled();

    
    Task<Integer> getInstalledCount();

    
    boolean isAvailable();

    
    Task<Void> installManager();

    
    Task<Void> removeManager();

    
    String getDisplayName();
}

