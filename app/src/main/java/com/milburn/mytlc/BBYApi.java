package com.milburn.mytlc;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BBYApi extends AsyncTask<String, Void, String> {

    public AsyncResponse delegate = null;
    private Context context;

    public BBYApi(Context con, AsyncResponse asyncResponse) {
        context = con;
        delegate = asyncResponse;
    }

    public interface AsyncResponse {
        void processFinish(String address);
    }

    private class Data {
        private List<Info> stores;
    }

    private class Info {
        private String address;
        private String city;
        private String region;
        private String postalCode;

        private String getCombinedAddress() {
            Collection<String> stringCollection = Arrays.asList(address, city, region, postalCode);
            String joined = StringUtil.join(stringCollection, ", ");
            return joined;
        }
    }

    @Override
    protected String doInBackground(String... params) {
        String url = "https://api.bestbuy.com/v1/stores(storeId=" + params[0].replaceFirst("^0+(?!$)", "") + ")?format=json&show=address,city,region,postalCode&apiKey=" + context.getString(R.string.bbyapi);
        String result;
        try {
            result = Jsoup.connect(url).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Gson gson = new Gson();
        Data data = gson.fromJson(result, Data.class);
        if (data != null && !data.stores.isEmpty()) {
            String combined = data.stores.get(0).getCombinedAddress();
            return combined;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        delegate.processFinish(result);
    }
}
