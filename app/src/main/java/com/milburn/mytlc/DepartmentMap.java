package com.milburn.mytlc;

import java.util.HashMap;

public class DepartmentMap {

    private HashMap<String, String> deptMap;

    public DepartmentMap() {
        deptMap = new HashMap<>();

        deptMap.put("61405", "Front of Precinct");
        deptMap.put("61100", "Home & Mobile Ent.");
        deptMap.put("50800", "Back of Precinct");
        deptMap.put("51600", "GSI");
        deptMap.put("51700", "Double Agent");
        deptMap.put("60020", "Management");
        deptMap.put("60025", "Sales Support Leaders");
        deptMap.put("60030", "Checkout");
        deptMap.put("60040", "Gaming");
        deptMap.put("60050", "Merch");
        deptMap.put("60060", "Sales Support");
        deptMap.put("60080", "Asset Protection");
        deptMap.put("60605", "Apple Master - Computing");
        deptMap.put("61101", "Lifestyles");
        deptMap.put("61102", "Solution Central");
        deptMap.put("61410", "Customer Service");
        deptMap.put("61420", "Appliances");
        deptMap.put("61430", "Computers");
        deptMap.put("61435", "Home Leaders");
        deptMap.put("61450", "Mobile Electronics");
        deptMap.put("61460", "Mobile");
        deptMap.put("61470", "Home Theater");
        deptMap.put("61480", "Digital Imaging");
        deptMap.put("61520", "Connected Devices");
        deptMap.put("61530", "Tablets");
        deptMap.put("61540", "Connectivity Leaders");
    }

    public String getDeptName(String dept) {
        if (deptMap.containsKey(dept)) {
            return deptMap.get(dept);
        }
        return dept;
    }
}
