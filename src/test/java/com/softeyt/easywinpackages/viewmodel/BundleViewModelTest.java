package com.softeyt.easywinpackages.viewmodel;

import com.softeyt.easywinpackages.model.BundleEntry;
import com.softeyt.easywinpackages.model.Package;
import com.softeyt.easywinpackages.model.PackageSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleViewModelTest {

    @Test
    void addEntry_skipsDuplicateEntriesForSamePackage() {
        BundleViewModel viewModel = new BundleViewModel(null);
        BundleEntry entry = new BundleEntry("Git.Git", "Git", PackageSource.WINGET, "2.49.0");

        viewModel.addEntry(entry);
        viewModel.addEntry(new BundleEntry("git.git", "Git", PackageSource.WINGET, "2.49.0"));

        assertEquals(1, viewModel.getEntries().size());
    }

    @Test
    void containsEntryForPackage_matchesBySourceAndIdentifierIgnoringCase() {
        BundleViewModel viewModel = new BundleViewModel(null);
        viewModel.addEntry(new BundleEntry("Git.Git", "Git", PackageSource.WINGET, "2.49.0"));

        Package pkg =
                new Package("git.git", "Git", "2.49.0", PackageSource.WINGET);

        assertTrue(viewModel.containsEntryForPackage(pkg));
    }
}

