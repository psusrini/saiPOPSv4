/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4.drivers;

import static ca.mcmaster.saipopsv4.Constants.*;  
import ca.mcmaster.saipopsv4.constraints.SimpleConstraint;
import ca.mcmaster.saipopsv4.heuristics.POPS_Heuristic;
import ilog.concert.IloNumVar;
import static java.lang.System.exit;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class TestDriver {
     
    public  static Logger logger;
     
    static   {
        logger=Logger.getLogger(TestDriver.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+TestDriver.class.getSimpleName()+ LOG_FILE_EXTENSION);
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
        
        System.out.println("Start Cplex with TEST driver ..." );
        
        Solver solver = new Solver (logger, DriverEnum.POPS) ;
        
        TreeMap<String, Double>  objectiveFunctionMap = solver.objectiveFunctionMap;
        TreeMap<String, IloNumVar>  mapOfAllVariablesInTheModel = solver.mapOfAllVariablesInTheModel;
        TreeMap<Integer, HashSet<SimpleConstraint>> mapOfAllConstraintsInTheModel = solver.mapOfAllConstraintsInTheModel;
        
        TreeMap<String, Boolean> fixings = new TreeMap<String, Boolean>();
        TreeMap<String, Double>  freeVariables = new TreeMap<String, Double> ();
        TreeSet<String> fractionalvariables = new TreeSet<String> ();
        
        fractionalvariables.add ("x1");
        fractionalvariables.add ("x2");
        fractionalvariables.add ("x3");
        fractionalvariables.add ("x4");
        fractionalvariables.add ("x5");
        fractionalvariables.add ("x6");
        fractionalvariables.add ("x7");
        
        freeVariables.put("x1", 0.5);
        freeVariables.put("x2", 0.5);
        freeVariables.put("x3", 0.5);
        freeVariables.put("x4", 0.5);
        freeVariables.put("x5", 0.5);
        freeVariables.put("x6", 0.5);
        freeVariables.put("x7", 0.5);
         
        String branchingVar =  (new POPS_Heuristic(fixings, freeVariables,fractionalvariables,
                        mapOfAllConstraintsInTheModel, objectiveFunctionMap))
                        .getBranchingVariable(  );
        
    }//end main
     
   
    
}
