package com.clemcab.diskoverindoors.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    public MutableLiveData<String> qrCode = new MutableLiveData<>();

    public void setQrCode(String scannedQrCode) {
        qrCode.setValue(scannedQrCode);
    }

    public HomeViewModel() {
    }

}