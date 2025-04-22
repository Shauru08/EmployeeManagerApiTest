/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.employee_manager_api.util;

/**
 *
 * @author loren
 */
public class FormatUtils {

    public static String jsonMessage(String key, String value) {
        return String.format("{\"%s\":\"%s\"}", key, value.replace("\"", "\\\""));
    }

}
