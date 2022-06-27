/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.some.tcp;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author me
 */
public class DateUtils {

	public static String getDateStr() {
		return new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss").format(new Date());
	}
}
