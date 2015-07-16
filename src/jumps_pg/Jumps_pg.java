/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jumps_pg;

import com.nuuptech.replicator.transfer.TransferAndFileIntegrity;
import com.nuuptech.replicator.transfer.TransferException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Leonel Vazquez Jasso
 */
public class Jumps_pg {

    static String archivo;
    static String host;
    static String usuario;
    static String directorioDestino;
    static int puerto;
    static String identidad;
    static String job;
    static int veces;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        usuario     = args[2];
        //identidad   = args[3];
        host        = args[0];
        puerto      = Integer.parseInt(args[1]);
        directorioDestino  = args[3];
        archivo     = args[4];
        veces       = Integer.parseInt(args[5]);
        job         = args[6];
        
        
        TransferAndFileIntegrity transfer = new TransferAndFileIntegrity();
        try {
            String valor = transfer.TransferAndFileIntegrity(host, puerto, usuario, directorioDestino, archivo, veces, job);
            //System.out.println("Average speed (KB/s): " + valor);
            
        } catch (TransferException ex) {
            Logger.getLogger(Jumps_pg.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Jumps_pg.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
