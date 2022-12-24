/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv4;
 

/**
 *
 * @author tamvadss
 */
public class Parameters {
    
    public static final int  MIP_EMPHASIS = 3;
     
    public static final boolean  USE_TED_FOR_BCP = false;
    
     
    //cplex config related
    public static boolean USE_STRONG_BRANCHING =false;    
    public static final int  HEUR_FREQ  = -1 ;    
    public static final int  FILE_STRATEGY= 3;  
    public static final int MAX_THREADS =  System.getProperty("os.name").toLowerCase().contains("win") ? 1 : 32;
    public static boolean USE_BARRIER_FOR_SOLVING_LP = false;
  
    
    public static final int MAX_TEST_DURATION_HOURS =10;
    public static final int  BRANCHING_OVERRULE_CYLES =2;
    
    public static final String PRESOLVED_MIP_FILENAME =              
            System.getProperty("os.name").toLowerCase().contains("win") ?
           "F:\\temporary files here recovered\\Purolator_LTL8.pre.sav.lp":
          //   "F:\\temporary files here recovered\\opm2-z12-s14.pre.sav":
         //    "F:\\temporary files here recovered\\p6b.pre.sav":
         //           "F:\\temporary files here recovered\\bab1.pre.sav":
       // "F:\\temporary files here recovered\\knapsackPOPS.lp":
            "PBO.pre.sav";
    
           
    //for perf variability testing  
    public static final long PERF_VARIABILITY_RANDOM_SEED = 1;
    public static final java.util.Random  PERF_VARIABILITY_RANDOM_GENERATOR =             
            new  java.util.Random  (PERF_VARIABILITY_RANDOM_SEED);   
    
    
}
