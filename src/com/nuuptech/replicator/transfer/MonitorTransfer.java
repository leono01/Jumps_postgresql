/*
 * Copyright 2014 Nuuptech S.A. de C.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nuuptech.replicator.transfer;

import com.jcraft.jsch.SftpProgressMonitor;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author Leonel Vazquez Jasso leonvj2.8@gmail.com
 */
class MonitorTransfer implements SftpProgressMonitor{
    
    private static final BigDecimal MILISECONDS = new BigDecimal(1000);
    private static final BigDecimal BITS = new BigDecimal(1024);
    private static final String     FILE_CAN_NOT_BE_EMPTY = "File can not be empty";
    private static final String     NOT_A_VALID_SOURCE_DIRECTORY = "Not a valid source directory";    
    
    private long        count = 0;
    private long        max = 0;
    private BigDecimal  start;        
    public  BigDecimal  velocidadBps;
        
    private String source;
    public  String destination;
    
    /**
     * 
     * Monitors the file transfer
     *
     * @param fileSize the size of the file we send to the remote host.
     * @param src the source path file.          
     * @throws TransferException
     */
    MonitorTransfer(long fileSize, String src) throws TransferException{
        if(fileSize <= 0){
            throw new TransferException(FILE_CAN_NOT_BE_EMPTY);
        }else{
            this.max = fileSize;
        }
        if(src == null){
            throw new TransferException(NOT_A_VALID_SOURCE_DIRECTORY);
        }else{
            this.source = src;
        }
    }
    
    @Override
    public void init(int op, java.lang.String src, java.lang.String dest, long max){
        this.destination = dest;        
        System.out.println("Subiendo archivo: " + this.source + " -> " + dest + " total: " + this.max);
        start = new BigDecimal(System.currentTimeMillis());        
    }
    private long percent = -1;
    
    @Override
    public boolean count(long bytes) {        
        this.count += bytes;        
        if (percent >= this.count * 100 / max) {
            return true;
        }
        percent = this.count * 100 / max;
        for (int x = 0; x < bytes; x++) {
            velocidadBps = new BigDecimal(this.count).divide((new BigDecimal(System.currentTimeMillis()).divide(MILISECONDS,6,RoundingMode.HALF_UP)).subtract(start.divide(MILISECONDS,6,RoundingMode.HALF_UP)),6, RoundingMode.HALF_UP);
            //System.out.print("Completed " + this.count + " bytes (" + percent + "%) of " + max + " bytes   " + velocidadBps.divide(BITS,2,RoundingMode.HALF_UP)+"KB/s\r");
        }
        return (true);
    }

    @Override
    public void end() {        
        System.out.println("\nFINISHED! in " + new BigDecimal(System.currentTimeMillis()).subtract(start).divide(MILISECONDS,2,RoundingMode.HALF_UP) + " seconds.");        
    }
    
}
