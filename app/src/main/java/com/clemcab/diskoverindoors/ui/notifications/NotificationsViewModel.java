package com.clemcab.diskoverindoors.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    public MutableLiveData<NavigationData> navData;

    public NotificationsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Notifications fragment");
    }

    public void setNavData(NavigationData navData) {
        this.navData = new MutableLiveData<>();
        this.navData.setValue(navData);
    }

    public LiveData<String> getText() {
        return mText;
    }
}