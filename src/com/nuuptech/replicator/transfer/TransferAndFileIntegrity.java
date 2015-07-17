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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Leonel Vazquez Jasso leonvj2.8@gmail.com
 */
public class TransferAndFileIntegrity implements Runnable {

    //private static final String FILE_TO_TRANSFER = "/var/uploads/replicador/files/3m.JPG";
    private static final BigDecimal BITS = new BigDecimal(1024);
    private static final String NOT_A_VALID_HOST = "Not a valid host.";
    private static final String NOT_A_VALID_PORT = "Not a valid port.";
    private static final String NOT_A_VALID_USER = "Not a valid user.";
    private static final String NOT_A_VALID_PASSWORD = "Not a valid password.";
    private static final String NOT_A_VALID_INSTALLATIONDIR = "Not a valid installation directory.";
    private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";
    private static final String NUMBER_OF_COPIES_MUST_BE_AT_LEAST_ONE = "Number of copies must be at least one";
    private static final String IP_RANGE_IS_BETWEEN_0_AND_255 = "IP range is between 0 and 255";
    private static final String NO = "no";
    private static final String SFTP = "sftp";
    private static final String MD5SUM = "md5sum ";
    private static final String EXEC = "exec";
    private static final Integer MD5LENGTH = 32;
    private static final String DELIMITER = " ";
    private static final String SOURCE = "Origen: ";
    private static final String DESTINATION = "Destino: ";
    private static final String SUCCESS = "El archivo se envió integramente.";
    private static final String YES = "0";
    private static final String NO2 = "1";
    private static final String IP_ADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static final String VALID_IP = "Valid IP: ";
    
    //private static final String SFTPPASSPROD = "RedHat2014!"; //host1
    private static final String SFTPPASSDRP = "sei2014";        //host 2 y 3
    
    //private static final String SFTPPASS = "n0t13n3!";

    private static MonitorTransfer monitorTransfer;
    private static BigDecimal plus = new BigDecimal(0);
    private static String averageUploadBandwidthAndIntegrity;
    private static String fechaActual = "";
    private static String exitCode = "EXIT_CODE=0";

    /**
     * Transfers a certain number of copies of a file to measure the network
     * bandwidth upload and checks the integrity of the file.
     *
     * @param host the IP address of the remote host
     * @param port the port number for sftp connection to the remote host
     * @param user the user for sftp and ssh connection to remote host
     * @param identity the public certificate for sftp and ssh connection to
     * remote host
     * @param destinationDirectory the destination directory where we send the
     * file on the remote host
     * @param fileToTransfer the file to transfer to the destination directory
     * @param timesToSendFile the number of copies of the file that we want to
     * send on the remote host
     * @return averageUploadBandwidthAndIntegrity the average upload bandwidth
     * (in kiloBytes) and integrity
     * @throws TransferException
     */
    //public String TransferAndFileIntegrity(String host, Integer port, String user, String identity, String destinationDirectory, String fileToTransfer, Integer timesToSendFile) throws TransferException {
    public String TransferAndFileIntegrity(String host, Integer port, String user, String destinationDirectory, String fileToTransfer, Integer timesToSendFile, String job) throws TransferException, Exception {
        if (host == null) {
            exitCode = "EXIT_CODE=1";
            System.out.println(exitCode);
            throw new TransferException(NOT_A_VALID_HOST);
        } else {
            // Create a Pattern object
            Pattern r = Pattern.compile(IP_ADDRESS_PATTERN);

            // Now create matcher object.
            Matcher m = r.matcher(host);

            if (m.find()) {
                System.out.println(VALID_IP + m.group(0));
            } else {
                throw new TransferException(IP_RANGE_IS_BETWEEN_0_AND_255);
            }

        }
        if (port != 22) {
            exitCode = "EXIT_CODE=1";
            System.out.println(exitCode);
            throw new TransferException(NOT_A_VALID_PORT);
        }
        if (user == null) {
            exitCode = "EXIT_CODE=1";
            System.out.println(exitCode);
            throw new TransferException(NOT_A_VALID_USER);
        }
        /**
         * if (identity == null) { throw new
         * TransferException(NOT_A_VALID_PASSWORD); }*
         */
        if (destinationDirectory == null) {
            exitCode = "EXIT_CODE=1";
            System.out.println(exitCode);
            throw new TransferException(NOT_A_VALID_INSTALLATIONDIR);
        }
        if (timesToSendFile <= 0) {
            exitCode = "EXIT_CODE=1";
            System.out.println(exitCode);
            throw new TransferException(NUMBER_OF_COPIES_MUST_BE_AT_LEAST_ONE);
        }

        fechaActual = fecha();
        System.out.println(""+job+"|"+fechaActual+"|root|PG-037|Empezando el envio del archivo de respaldo al DRP...|Jumps_pg.jar|PG-ERP-01");
                
        
        BigDecimal average = new BigDecimal(0);
        String command;
        String tmp;
        String md5Source = "";
        String md5Destination = "";

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        ChannelExec channelExec = null;
        ExecuteConsoleCommand running = new ExecuteConsoleCommand();

        StringTokenizer st;
        String token;

        try {
            JSch.setLogger(new Recorder());
            JSch jsch = new JSch();
            //jsch.addIdentity(identity);
            session = jsch.getSession(user, host, port);
            
            
            //session.setPassword(SFTPPASSPROD);
            session.setPassword(SFTPPASSDRP);
            //session.setPassword(SFTPPASS);
            
            java.util.Properties config = new java.util.Properties();
            config.put(STRICT_HOST_KEY_CHECKING, NO);
            session.setConfig(config);
            session.connect();

            channel = session.openChannel(SFTP);
            channel.connect();
            channelSftp = (ChannelSftp) channel;
            channelSftp.cd(destinationDirectory);
            command = MD5SUM + fileToTransfer;
            tmp = running.ExecutingCommand(command);
            st = new StringTokenizer(tmp, DELIMITER, true);
            while (st.hasMoreTokens()) {
                token = st.nextToken().trim();
                if (token.length() == MD5LENGTH) {
                    md5Source = token.trim();
                    System.out.println(SOURCE + md5Source);
                }
            }
            File f = new File(fileToTransfer);

            for (int x = 1; x <= timesToSendFile; x++) {
                channelSftp.put(new FileInputStream(f), f.getName(), monitorTransfer = new MonitorTransfer(f.length(), fileToTransfer));
                plus = plus.add(monitorTransfer.velocidadBps);
                if (x == timesToSendFile) {
                    average = plus.divide(new BigDecimal(timesToSendFile), 2, RoundingMode.HALF_UP);
                }
            }

            channelExec = (ChannelExec) session.openChannel(EXEC);

            channelExec.setCommand(MD5SUM + monitorTransfer.destination);


            channelExec.connect();

            String msg = null;
            BufferedReader in;

            try {
                in = new BufferedReader(new InputStreamReader(channelExec.getInputStream()));
                while ((msg = in.readLine()) != null) {
                    st = new StringTokenizer(msg, DELIMITER, true);
                    while (st.hasMoreTokens()) {
                        token = st.nextToken();
                        if (token.length() == MD5LENGTH) {
                            md5Destination = token.trim();
                            System.out.println(DESTINATION + md5Destination);
                        }
                    }
                    System.out.println(msg);
                }
            } catch (IOException ex) {
                Logger.getLogger(TransferAndFileIntegrity.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (md5Source.equals(md5Destination)) {
                System.out.println(SUCCESS);
                //averageUploadBandwidthAndIntegrity = average.toString() + YES;
                averageUploadBandwidthAndIntegrity = YES;
                channelExec.disconnect();
                channel.disconnect();
                session.disconnect();
                
                System.out.println(exitCode);
                fechaActual = fecha();
                System.out.println(""+job+"|"+fechaActual+"|root|PG-038|Terminó el envio del archivo de respaldo.|Jumps_pg.jar|PG-ERP-01");
                
                ContinuaSalto(host, port, user, fileToTransfer, averageUploadBandwidthAndIntegrity,job);
            } else {
                //averageUploadBandwidthAndIntegrity = average.toString() + NO2;
                averageUploadBandwidthAndIntegrity = NO2;
                channelExec.disconnect();
                channel.disconnect();
                session.disconnect();
                                
                fechaActual = fecha();
                System.out.println(""+job+"|"+fechaActual+"|root|PG-038|Terminó el envio del archivo de respaldo.|Jumps_pg.jar|PG-ERP-01");
                
                ContinuaSalto(host, port, user, fileToTransfer, averageUploadBandwidthAndIntegrity,job);
            }

            //averageUploadBandwidthAndIntegrity = average.toString();
        } catch (JSchException ex) {
            ex.printStackTrace();
            Logger.getLogger(TransferAndFileIntegrity.class.getName()).log(Level.SEVERE, null, ex);
            
            System.out.println(ex);
            fechaActual = fecha();
            System.out.println(""+job+"|"+fechaActual+"|root|PG-038|Error al enviar el archivo de respaldo.|Jumps_pg.jar|PG-ERO-01");
            System.out.println("EXIT_CODE=1");
            System.exit(1);
        
        } catch (SftpException ex) {
            ex.printStackTrace();
            Logger.getLogger(TransferAndFileIntegrity.class.getName()).log(Level.SEVERE, null, ex);
            
            System.out.println(ex);
            fechaActual = fecha();
            System.out.println(""+job+"|"+fechaActual+"|root|PG-038|Error al enviar el archivo de respaldo.|Jumps_pg.jar|PG-ERO-01");
            System.out.println("EXIT_CODE=1");
            System.exit(1);
        
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            Logger.getLogger(TransferAndFileIntegrity.class.getName()).log(Level.SEVERE, null, ex);
            
            System.out.println(ex);
            fechaActual = fecha();
            System.out.println(""+job+"|"+fechaActual+"|root|PG-038|Error al enviar el archivo de respaldo.|Jumps_pg.jar|PG-ERO-01");
            System.out.println("EXIT_CODE=1");
            System.exit(1);
        }

        
        
        return averageUploadBandwidthAndIntegrity;
    }

    public void ContinuaSalto(String host, Integer port, String user, String fileToTransfer, String condicion, String job) throws TransferException {
        if (condicion.equals(YES)) {

            Session session = null;
            //Channel channel = null;
            ChannelExec channelExec = null;
            


            try {
                JSch.setLogger(new Recorder());
                JSch jsch = new JSch();
                //jsch.addIdentity(identity);
                session = jsch.getSession(user, host, port);
                
                
                //session.setPassword(SFTPPASSPROD);
                session.setPassword(SFTPPASSDRP);
                
                //session.setPassword(SFTPPASS);
                
                java.util.Properties config = new java.util.Properties();
                config.put(STRICT_HOST_KEY_CHECKING, NO);
                session.setConfig(config);
                session.connect();

                channelExec = (ChannelExec) session.openChannel(EXEC);
                
                
                //Comando que se ejecutará en MAQUINA 2 conectado desde MAQUINA 1
                //channelExec.setCommand("java -jar /root/pg/dist/Jumps_pg.jar 10.10.10.98 22 root /var/uploads/replicator/postgres/files /var/uploads/replicator/postgres/files/backup_.tar.gz 1 "+job);                
                
                //Comando que se ejecutará en MAQUINA 3 conectado desde MAQUINA 2
                //channelExec.setCommand("java -jar /root/pg/dist/Jumps_pg.jar 10.10.9.153 22 root /media/Respaldos/postgres/files /var/uploads/replicator/postgres/files/backup_.tar.gz 1 "+job);                
                
                // Se lista el direcotorio donde se recibe el archivo
                channelExec.setCommand("ls -la /media/Respaldos/postgres/files");
                
                System.out.println(exitCode);
                
                channelExec.connect();

                String msg = null;
                BufferedReader in;

                try {
                    in = new BufferedReader(new InputStreamReader(channelExec.getInputStream()));
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TransferAndFileIntegrity.class.getName()).log(Level.SEVERE, null, ex);
                }
                

                channelExec.disconnect();
                //channel.disconnect();
                session.disconnect();

            } catch (JSchException ex) {
                ex.printStackTrace();
                Logger.getLogger(TransferAndFileIntegrity.class.getName()).log(Level.SEVERE, null, ex);
                
                System.out.println(ex);
                System.out.println(""+job+"|"+fechaActual+"|root|PG-038|Error al enviar el respaldo.|Jumps_pg.jar|PG-ERO-01");
                System.out.println("EXIT_CODE=1");
                System.exit(1);
            }

        } else {
            System.out.println(""+job+"|"+fechaActual+"|root|PG-039|Algo sucedió que no se envió integramente el respaldo.|Jumps_pg.jar|PG-ERO-01");
            System.out.println("EXIT_CODE=1");
            System.exit(1);
        }

    }

    @Override
    public void run() {

    }
    
    public String fecha() throws Exception {
        String fechaActual;
        try {
            DateFormat format = new SimpleDateFormat(" dd/MM/yyyy | HH:mm:ss ");
            fechaActual=format.format(new Date());
            
        } catch (Exception exc) {
            System.out.printf("Can't be formatted!", exc);
            throw exc;
        }
        return fechaActual;
    }

}
