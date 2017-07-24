package com.milburn.mytlc;

import java.util.HashMap;

public class DepartmentMap {

    private HashMap<String, String> deptMap;

    public DepartmentMap() {
        deptMap = new HashMap<>();

        deptMap.put("50700", "Back of Precinct");
        deptMap.put("50800", "Autotech");
        deptMap.put("60025", "Geek Squad Leadership");
        deptMap.put("60030", "Front End");
        deptMap.put("60050", "Merch");
        deptMap.put("60060", "Inventory");
        deptMap.put("60080", "Asset Protection");
        deptMap.put("61100", "Home Theater");
        deptMap.put("61101", "Lifestyes");
        deptMap.put("61405", "Front of Precinct");
        deptMap.put("61410", "Phones");
        deptMap.put("61420", "Appliances");
        deptMap.put("61430", "Computing");
        deptMap.put("61450", "Operations Leadership");
        deptMap.put("61460", "Mobile");
        deptMap.put("61480", "Digital Imaging");
        deptMap.put("61520", "Connected Devices");
    }

    public String getDeptName(String dept) {
        if (deptMap.containsKey(dept)) {
            return deptMap.get(dept);
        }
        return dept;
    }
}
