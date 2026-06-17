/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kiokle.breadsite;

/**
 *
 * @author me
 */
public class HttpResponse {

    private final byte[] data;
    private final byte[] headers;

    public HttpResponse(byte[] data, byte[] headers) {
        this.data = data;
        this.headers = headers;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getHeaders() {
        return headers;
    }
}
