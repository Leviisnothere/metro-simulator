import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Sim {


  public static void run_sim(MTB MTB, Log log, int simulationDelay) {

    final Lock trainPassengerLock = new ReentrantLock();
    final Lock trainsDirectionLock = new ReentrantLock();
    final Lock transPosLock = new ReentrantLock();
    final Lock stationTrainLock = new ReentrantLock();
    final Lock passengerPosLock = new ReentrantLock();

    final Map<Station,Lock> stationsTrainArriveLocks = new HashMap<>();
    final Map<Station,Lock> stationsEmptyLocks = new HashMap<>();
    final Map<Station, Condition> trainArrive = new HashMap<>();
    final Map<Station,Condition> stationEmpty = new HashMap<>();

    for(List<Station> stations: MTB.trainsStops.values()){
      for(Station s: stations){
        final Lock lock1 = new ReentrantLock();
        final Lock lock2 = new ReentrantLock();
        stationsTrainArriveLocks.putIfAbsent(s,lock1);
        stationsEmptyLocks.putIfAbsent(s,lock2);
        trainArrive.putIfAbsent(s,lock1.newCondition());
        stationEmpty.putIfAbsent(s,lock2.newCondition());
      }
    }

    class PassengerThread implements Runnable{
      Passenger p;
      int pos, destination_pos;
      Train t;

      public PassengerThread(Passenger passenger){
        this.p = passenger;
        this.pos = 0;
        this.destination_pos = MTB.passengerTrips.get(p).size() -1;
        this.t = null;

      }
      @Override
      public void run() {
        while(pos != destination_pos){
          Station s = MTB.passengerPos.get(p);
          Station next_s = MTB.passengerTrips.get(p).get(pos+1);
          if(t != null){
            //passenger waiting for train to arrive at next destination
            stationsTrainArriveLocks.get(next_s).lock();
            try {
              trainArrive.get(next_s).await();
              if(MTB.stationTrain.get(next_s).equals(t)){

                trainPassengerLock.lock();
                MTB.trainPassenger.get(t).remove(p);
                trainPassengerLock.unlock();

                log.passenger_deboards(p,t,next_s);
                pos +=1;
                this.t = null;
              }
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }finally{
              stationsTrainArriveLocks.get(next_s).unlock();
            }
          }
          else{
            //passenger waiting for train to arrive at station
            stationsTrainArriveLocks.get(s).lock();
            try {
              trainArrive.get(s).await();

              this.t = MTB.stationTrain.get(s);

              //board only if train has passenger's next_stop forward.
              if(MTB.hasStopForward(t,next_s)) {

                log.passenger_boards(p, t, s);
                trainPassengerLock.lock();
                MTB.trainPassenger.get(t).add(p);
                trainPassengerLock.unlock();
              }
              else{
                this.t=null;
              }

            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }finally{
              stationsTrainArriveLocks.get(s).unlock();
            }
          }
        }

      }
    }
    class TrainThread implements Runnable{
      Train t;
      int pos;
      int endPos;
      public TrainThread(Train train){
        this.t = train;
        this.pos = 0;
        this.endPos = MTB.trainsStops.get(t).size() -1;
      }

      @Override
      public void run() {
        while(true){

          Station s1,s2;
          int advance = MTB.trainsDirection.get(t) ? 1:-1;
          s1 = MTB.trainsPos.get(t);
          s2 = MTB.trainsStops.get(t).get(pos+advance);
          stationsTrainArriveLocks.get(s1).lock();
          trainArrive.get(s1).signalAll();
          stationsTrainArriveLocks.get(s1).unlock();
          try {
            Thread.sleep(10);
          }catch(InterruptedException e){
            throw new RuntimeException(e);
          }

          stationsEmptyLocks.get(s2).lock();
          try{
            if(MTB.stationTrain.get(s2)!=null) {
              stationEmpty.get(s2).await();
            }
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }finally{
            stationsEmptyLocks.get(s2).unlock();
          }

          //lock

          if(pos+advance == 0 || pos+advance == endPos){
            Boolean direction = (pos + advance) == 0;
            trainsDirectionLock.lock();
            MTB.trainsDirection.put(t,direction);
            trainsDirectionLock.unlock();
          }
          pos += advance;

          transPosLock.lock();
          MTB.trainsPos.put(t,s2);
          transPosLock.unlock();

          stationTrainLock.lock();
          MTB.stationTrain.put(s2,t);
          MTB.stationTrain.put(s1,null);
          stationTrainLock.unlock();

          log.train_moves(t,s1,s2);
          stationsEmptyLocks.get(s1).lock();
          stationEmpty.get(s1).signalAll();
          stationsEmptyLocks.get(s1).unlock();

          passengerPosLock.lock();
          for(Passenger p : MTB.trainPassenger.get(t)){
            MTB.passengerPos.put(p,s2);
          }
          passengerPosLock.unlock();

          try {
            Thread.sleep(simulationDelay* 1000L);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          //unlock
        }
      }
    }
    ExecutorService trainsManager = Executors.newFixedThreadPool(MTB.trainsStops.size());
    ExecutorService passengerManager = Executors.newFixedThreadPool(MTB.passengerTrips.size());
    for(Train t: MTB.trainsStops.keySet()){
      trainsManager.submit(new TrainThread(t));
    }
    for(Passenger p: MTB.passengerTrips.keySet()){
      passengerManager.submit(new PassengerThread(p));
    }
    passengerManager.shutdown();
    while(!passengerManager.isTerminated()){}
    trainsManager.shutdownNow();
  }



  public static void main(String[] args) throws Exception {
    Scanner scanner = new Scanner(System.in);
    int simulationDelay = 2;
    System.out.println("simulation delay on a scale of 0 to 10.\n" +
            " It is recommended to enter a integer value between 2 to 3 for better observation of the simulation process,\n" +
            "entering 0 will result in the simulation to finish almost instantly");
    System.out.print("Please enter a simulation delay(0 to 10):");
    simulationDelay = scanner.nextInt();


    JFrame frame = new JFrame("Train Simulator");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    JPanel MTAPanel = new JPanel();

    MTB MTB = new MTB(MTAPanel);
    MTB.loadConfig("nyc_mtb.json");
    System.out.println("loadConfig finished");

    Log log = new Log();

    
    
    final int sim_delay = simulationDelay;
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("Menu");
    JMenuItem resetFundMenuItem = new JMenuItem("balance reset");
    resetFundMenuItem.addActionListener(new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		for(Passenger p: MTB.passengerTrips.keySet()) {
    			MetroCardDatabase.updateMetroCardBalance(MTB.passengerMetroCard.get(p.toString()), 100);
    			System.out.println(p.toString() + "'s" + "metroCard balance has been reset to $100");
    		}
    	}
    });
    JMenuItem exitMenuItem = new JMenuItem("Exit");
    exitMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    });
    fileMenu.add(resetFundMenuItem);
    fileMenu.add(exitMenuItem);
    menuBar.add(fileMenu);
    frame.setJMenuBar(menuBar);
    
    frame.add(MTAPanel);
    frame.setSize(800, 700);
    frame.setVisible(true);
    
    run_sim(MTB, log, sim_delay);
    System.out.println("All passengers arrived at their destination, simulation ended");
    String s = new LogJson(log).toJson();
    PrintWriter out = null;
    try {
      out = new PrintWriter("log.json");
    } catch (FileNotFoundException e1) {
      throw new RuntimeException(e1);
      }
    out.print(s);
    out.close();
    
    
    
    
  }
}
