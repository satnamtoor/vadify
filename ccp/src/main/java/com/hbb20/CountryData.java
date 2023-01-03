package com.hbb20;

import static com.hbb20.CCPCountry.DEFAULT_FLAG_RES;

public class CountryData {
    String nameCode;
    String phoneCode;
    String name;
    String englishName;
    String languageCode;

    int flagResID = DEFAULT_FLAG_RES;

    public CountryData(String nameCode, String phoneCode, String name, String englishName, int flagResID,String languageCode) {
        this.nameCode = nameCode;
        this.phoneCode = phoneCode;
        this.name = name;
        this.englishName = englishName;
        this.flagResID = flagResID;
        this.languageCode = languageCode;
    }

    public String getNameCode() {
        return nameCode;
    }

    public void setNameCode(String nameCode) {
        this.nameCode = nameCode;
    }

    public String getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public int getFlagResID() {
        return flagResID;
    }

    public void setFlagResID(int flagResID) {
        this.flagResID = flagResID;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}
