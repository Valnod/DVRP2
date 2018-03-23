package com.example.alex.spp;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by Alex on 22/03/2018.
 */

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                FragmentGeneralSettings generalSettings = new FragmentGeneralSettings();
                return generalSettings;
            case 1:
                FragmentPhotoSettings photoSettings = new FragmentPhotoSettings();
                return photoSettings;
            case 2:
                FragmentVideoSettings videoSettings = new FragmentVideoSettings();
                return videoSettings;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
