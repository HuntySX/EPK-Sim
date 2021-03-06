package com.company.Simulation.Simulation_Base.Data.Discrete_Data;

import com.company.EPK.*;
import com.company.Run.Discrete_Event_Generator;
import com.company.Simulation.Simulation_Base.Data.Shared_Data.Settings;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.company.Enums.Start_Event_Type.*;


//Main Class for the Simulation Event Calendar. This Class has a List of External Events, a List for Upcoming Events
//and the Timecode for the Simulation. Also this Class maintains all delayed Events of the simulation in a Waiting List
public class Event_Calendar {

    private int RuntimeDays;
    private LocalTime runtime;
    private final LocalTime Begin_Time;
    private final LocalTime End_Time;
    private final List<Simulation_Event_List> Upcoming_List;
    private final Simulation_Waiting_List Waiting_List;
    private final Settings settings;
    private int act_runtimeDay;
    private boolean finished_cycle;
    private final EPK epk;
    private final Discrete_Event_Generator Generator;
    private final List<Instance_Workflow> Activation_List;
    private int Unique_Waiting_Ticket_ID;
    private List<List<External_Event>> External_Events;

    public Event_Calendar(Settings settings, EPK epk, Discrete_Event_Generator generator) {

        this.settings = settings;
        this.epk = epk;
        finished_cycle = false;
        this.act_runtimeDay = 0;
        this.RuntimeDays = settings.getMax_RuntimeDays();
        this.Generator = generator;
        Begin_Time = settings.getBeginTime();
        End_Time = settings.getEndTime();
        runtime = LocalTime.of(Begin_Time.getHour(), Begin_Time.getMinute(), Begin_Time.getSecond());
        Waiting_List = new Simulation_Waiting_List();
        Upcoming_List = new ArrayList<>();
        Activation_List = new ArrayList<>();
        Unique_Waiting_Ticket_ID = 0;
        External_Events = new ArrayList<>();
        for (int i = 0; i < RuntimeDays; i++) {
            Simulation_Event_List Day_Upcoming_List = new Simulation_Event_List();
            Upcoming_List.add(Day_Upcoming_List);
        }

    }

    public int getUnique_Waiting_Ticket_ID() {
        int id = Unique_Waiting_Ticket_ID;
        Unique_Waiting_Ticket_ID++;
        return id;
    }

    public int getAct_runtimeDay() {
        return act_runtimeDay;
    }

    public void setAct_runtimeDay(int act_runtimeDay) {
        this.act_runtimeDay = act_runtimeDay;
    }

    public LocalTime getRuntime() {
        return runtime;
    }

    public void setRuntime(LocalTime runtime) {
        this.runtime = runtime;
    }

    public LocalTime getBegin_Time() {
        return Begin_Time;
    }

    public LocalTime getEnd_Time() {
        return End_Time;
    }

    public int getRuntimeDays() {
        return RuntimeDays;
    }

    public void incrementRuntimeDay() {
        RuntimeDays++;
    }

    public List<Simulation_Event_List> getUpcoming_List() {
        return Upcoming_List;
    }

    public Simulation_Event_List get_Single_Upcoming_List(int day) {
        return Upcoming_List.get(day);
    }

    public Simulation_Waiting_List getWaiting_List() {
        return Waiting_List;
    }

    public void Add_To_Waiting_List(Instance_Workflow Instance) {
        if (Instance != null) {
            Waiting_List.addTimedEvent(Instance);
        }
    }

    //Main method to place an new Instance_Workflow on the Upcoming List. Manages the Upcoming List to search for the
    //Day in wich the new Instance_Workflow should be placed. For this Day there is a Index on the List wich has all
    //Events for that day in a Simulation_Event_List Object.

    public void Add_To_Upcoming_List(Instance_Workflow Instance, int day) {

        if (Instance != null) {

            if (Instance.getTo_Start().isBefore(End_Time)) {
                Upcoming_List.get(day).addTimedEvent(Instance);
            } else if (Instance.getEPKNode() instanceof Function && Instance.isWorking()) {
                if (day + 1 < RuntimeDays) {
                    LocalTime Calculation = Instance.getTo_Start();
                    int to_finish = Calculation.toSecondOfDay() - End_Time.toSecondOfDay();
                    Instance.setTo_Start(Begin_Time.plusSeconds(to_finish));
                    Upcoming_List.get(day + 1).addTimedEvent(Instance);
                }
                //Else Drop
            }
        }
    }

    public Event_Instance calculate_Next_Event() {
        return null;
    }

    public void Remove_From_Upcoming_List(Instance_Workflow Instance, int day) {
        Upcoming_List.get(day).remove_from_EventList(Instance);
    }

    public void Remove_From_Waiting_List(Instance_Workflow Instance) {
        Waiting_List.remove_from_WaitingList(Instance);
    }


    //Quicksort Algorithm to Sort the Upcoming and Waiting lists on Adding a new Instance
    private static void QuickSort(int[] inputArray, int low, int high) {
        int iLowerIndex = low;
        int iHighIndex = high;
        // Take middle as pivot element.
        int middle = low + (high - low) / 2;
        int pivotElement = inputArray[middle];
        while (iLowerIndex <= iHighIndex) {
            // Keep scanning lower half till value is less than pivot element
            while (inputArray[iLowerIndex] < pivotElement) {
                iLowerIndex++;
            }
            // Keep scanning upper half till value is greater than pivot element
            while (inputArray[iHighIndex] > pivotElement) {
                iHighIndex--;
            }
            //swap element if they are out of place
            if (iLowerIndex <= iHighIndex) {
                swap(inputArray, iLowerIndex, iHighIndex);
                iLowerIndex++;
                iHighIndex--;
            }
        }
        // Sort lower half -- low to iHighIndex
        if (low < iHighIndex) {
            QuickSort(inputArray, low, iHighIndex);
        }
        // Sort upper half -- iLowerIndex to high
        if (iLowerIndex < high) {
            QuickSort(inputArray, iLowerIndex, high);
        }
    }

    //Fills the Calendar initialy on Begin of the Simulation. For this it Determines the begin and Endtime of each Day
    //and adds one new Index onto the UpcomingList for each Day. new Instances are Generated Based on a Distributon type
    //Random (Total Random Time Calculation, will mostly be Even Distributed), Exponential (places more Instances on the
    //end of a Day), Normal (Gaussian distribution per Day with the Middle between Begin and Endtime + a 25 % Quartile as
    //Standart Distribution.
    public void fillCalendar() {

        int days = RuntimeDays;
        int begintime = Begin_Time.toSecondOfDay();
        int endtime = End_Time.toSecondOfDay();
        List<Start_Event> Start_Events = epk.get_Discrete_Start_Events();
        for (Start_Event sv : Start_Events) {
            int counter_to_Instantiate = sv.getTo_Instantiate();
            if (sv.getStart_event_type() == NORMAL) {
                double Time_Upper_bound = End_Time.toSecondOfDay() - Begin_Time.toSecondOfDay();
                double Time_Middle_bound = Time_Upper_bound / 2;
                double time_Standart_deviation = Time_Middle_bound * 0.3;
                NormalDistribution Distribution = new NormalDistribution(Time_Middle_bound, time_Standart_deviation);
                for (int i = 0; i < RuntimeDays; i++) {
                    int[] timerlist = new int[counter_to_Instantiate];
                    for (int j = 0; j < counter_to_Instantiate; j++) {
                        int time_to_Generate = -1;
                        while (!(time_to_Generate >= Begin_Time.toSecondOfDay()
                                && time_to_Generate <= End_Time.toSecondOfDay())) {
                            int sample = (int) Distribution.sample();
                            time_to_Generate = sample + Begin_Time.toSecondOfDay();
                        }
                        timerlist[j] = time_to_Generate;
                    }
                    QuickSort(timerlist, 0, counter_to_Instantiate - 1);
                    for (int k : timerlist) {
                        Event_Instance new_Ev_Instance = new Event_Instance(Generator.get_Unique_case_ID());
                        new_Ev_Instance.add_To_Scheduled_Work(sv);
                        LocalTime to_Start = LocalTime.ofSecondOfDay(k);
                        Instance_Workflow to_Instantiate = new Instance_Workflow(new_Ev_Instance, to_Start, sv);
                        Upcoming_List.get(i).addTimedEvent(to_Instantiate);
                    }
                }

            } else if (sv.getStart_event_type() == RANDOM) {

                for (int i = 0; i < RuntimeDays; i++) {
                    int[] timerlist = new int[counter_to_Instantiate];
                    int duration = endtime - begintime;
                    Random rand = new Random();
                    for (int j = 0; j < counter_to_Instantiate; j++) {
                        int start_Time = rand.nextInt(duration);
                        start_Time = begintime + start_Time;
                        timerlist[j] = start_Time;
                    }
                    QuickSort(timerlist, 0, counter_to_Instantiate - 1);
                    for (int k : timerlist) {
                        Event_Instance new_Ev_Instance = new Event_Instance(Generator.get_Unique_case_ID());
                        new_Ev_Instance.add_To_Scheduled_Work(sv);
                        LocalTime to_Start = LocalTime.ofSecondOfDay(k);
                        Instance_Workflow to_Instantiate = new Instance_Workflow(new_Ev_Instance, to_Start, sv);
                        Upcoming_List.get(i).addTimedEvent(to_Instantiate);
                    }
                }
            } else if (sv.getStart_event_type() == EXPONENTIAL) {
                double Time_Upper_bound = End_Time.toSecondOfDay() - Begin_Time.toSecondOfDay();
                double Time_Middle_bound = Time_Upper_bound / 2;
                ExponentialDistribution Distribution = new ExponentialDistribution(Time_Middle_bound);
                for (int i = 0; i < RuntimeDays; i++) {
                    int[] timerlist = new int[counter_to_Instantiate];
                    for (int j = 0; j < counter_to_Instantiate; j++) {
                        int time_to_Generate = -1;
                        while (!(time_to_Generate >= Begin_Time.toSecondOfDay()
                                && time_to_Generate <= End_Time.toSecondOfDay())) {
                            int sample = (int) Distribution.sample();
                            time_to_Generate = End_Time.toSecondOfDay() - sample;
                        }
                        timerlist[j] = time_to_Generate;
                    }
                    QuickSort(timerlist, 0, counter_to_Instantiate - 1);
                    for (int k : timerlist) {
                        Event_Instance new_Ev_Instance = new Event_Instance(Generator.get_Unique_case_ID());
                        new_Ev_Instance.add_To_Scheduled_Work(sv);
                        LocalTime to_Start = LocalTime.ofSecondOfDay(k);
                        Instance_Workflow to_Instantiate = new Instance_Workflow(new_Ev_Instance, to_Start, sv);
                        Upcoming_List.get(i).addTimedEvent(to_Instantiate);
                    }
                }
            }
        /*else if(fillingType == ){

        }
        else if(fillingType == ){

        }*/


        }
    }

    public boolean isFinished_cycle() {
        return finished_cycle;
    }

    public void setFinished_cycle(boolean finished_cycle) {
        this.finished_cycle = finished_cycle;
    }

    //Jumps the Event Calendar to the next Second (right now). Can be Changed to Jump to the Next Event Time (tbd)
    //if the Jump returns a Value outside of the Timehorizon of the Simulation,
    // The Simulation is stopped through setFinished_cycle
    public void jump() {
        runtime = runtime.plusSeconds(1);
        if (runtime.isAfter(getEnd_Time())) {
            act_runtimeDay++;
            if (act_runtimeDay >= getRuntimeDays()) {
                setFinished_cycle(true);
                System.out.println("Finished Sim");
            } else {
                runtime = getBegin_Time();
                System.out.println("Day Jumped");
            }
        }
    }

    //Helper Method for Activating Functions to Instantiate a new Simulation instance on the connected
    //activating start event. Calculation for this is similar as the activate / deactivate Function Method in Simulator
    //as the Incoming Time of the new Simulation instance is calculated to work with the Design of the Upcoming list
    //(i.e. jump days if the instantiation Time is outside of the current Day).
    //Doesn??t instantiate if the Time to instantiate is Outside of the Simulation Horizon.
    public void instantiate_new_Activation_Event(Activating_Start_Event start, Activating_Function Func, Instance_Workflow for_Workflow, Workingtime to_Start) {
        Activating_Event_Instance activating_instance = new Activating_Event_Instance(Generator.get_Unique_case_ID(), Func, for_Workflow);
        LocalTime StartTime = getRuntime();

        int lasting_Shifttime_in_Seconds = getEnd_Time().toSecondOfDay() - getRuntime().toSecondOfDay();
        int Workingtime_in_Seconds = to_Start.get_Duration_to_Seconds();
        int advanceday = 0;

        if (Workingtime_in_Seconds <= lasting_Shifttime_in_Seconds) {
            Instance_Workflow activating_workflow = new Instance_Workflow(activating_instance, StartTime, start);
            Add_To_Upcoming_List(activating_workflow, getAct_runtimeDay());
        } else {
            Workingtime_in_Seconds = Workingtime_in_Seconds - lasting_Shifttime_in_Seconds;
            int Shifttime_in_Seconds = getEnd_Time().toSecondOfDay() - getBegin_Time().toSecondOfDay();
            advanceday++;

            while (Workingtime_in_Seconds > Shifttime_in_Seconds) {
                Workingtime_in_Seconds = Workingtime_in_Seconds - Shifttime_in_Seconds;
                advanceday++;
            }

            if (advanceday >= getRuntimeDays() - getAct_runtimeDay()) {
                // not instantiable in Runtime, Drop
            } else {
                advanceday = advanceday + getAct_runtimeDay();
                StartTime = getBegin_Time();
                StartTime.plusSeconds(Workingtime_in_Seconds);
                Instance_Workflow activating_workflow = new Instance_Workflow(activating_instance, StartTime, start);
                activating_workflow.getInstance().add_To_Scheduled_Work(start);
                Add_To_Upcoming_List(activating_workflow, getAct_runtimeDay() + advanceday);
            }
        }
    }

    //swap elements
    private static void swap(int[] arr, int iElement1, int iElement2) {
        int temp = arr[iElement1];
        arr[iElement1] = arr[iElement2];
        arr[iElement2] = temp;
    }

    public List<Instance_Workflow> getActivationList() {
        return Activation_List;
    }

    public void addToActivationList(Instance_Workflow workflow) {
        if (!Activation_List.contains(workflow)) {
            Activation_List.add(workflow);
        }
    }

    public void removeFromActivationList(Instance_Workflow workflow) {
        Activation_List.remove(workflow);
    }

    public LocalTime getNextInstanceTime(Instance_Workflow to_run) {
        for (Simulation_Event_List Days : Upcoming_List) {
            for (Instance_Workflow flow : Days.getWorkflows()) {
                if (flow.getInstance().equals(to_run.getInstance())) {
                    return flow.getTo_Start();
                }
            }
        }
        return null;
    }

    public int getNextInstanceDay(Instance_Workflow to_run) {
        int days = 0;
        for (Simulation_Event_List Days : Upcoming_List) {
            for (Instance_Workflow flow : Days.getWorkflows()) {
                if (flow.getInstance().equals(to_run.getInstance())) {
                    return days;
                }
            }
            days++;
        }
        return -1;
    }

    public List<List<External_Event>> getExternal_Events() {
        return External_Events;
    }

    public void setExternal_Events(List<List<External_Event>> external_Events) {
        External_Events = external_Events;
    }
}



