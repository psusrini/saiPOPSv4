package ca.mcmaster.saipopsv4.drivers;
 
import static ca.mcmaster.saipopsv4.Constants.*;
import static java.lang.System.exit;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author tamvadss
 */
public class POPS_Driver {
    
    public  static Logger logger;
     
    static   {
        logger=Logger.getLogger(POPS_Driver.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+POPS_Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);            
             
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
    
      
    
    public static void main(String[] args) throws Exception{
        
        System.out.println("Start Cplex with POPS heuristic version 4 ..." );
        
        Solver solver = new Solver (logger, DriverEnum.POPS) ;
         
        solver.solve ( );
        
    }//end main
     
   
    
}
