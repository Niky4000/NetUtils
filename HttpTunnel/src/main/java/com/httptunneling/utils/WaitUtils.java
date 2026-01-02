/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.httptunneling.utils;

/**
 *
 * @author me
 */
public class WaitUtils {

    private static final int SECOND = 1000;

    public static void waitSomeTime(int timeToWait) {
        while (true) {
            try {
                Thread.sleep(timeToWait * SECOND);
                break;
            } catch (InterruptedException ex) {
                continue;
            }
        }
    }
}
