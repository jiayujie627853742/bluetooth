package com.wmty.bluetooth.common;

import org.json.JSONObject;

/**
 * uflo
 * Created by Jack on 2017-11-07.
 */

public interface BluetoothInterface {

    void responseListener(JSONObject response);

    void errorListener(int code, String msg);

}
