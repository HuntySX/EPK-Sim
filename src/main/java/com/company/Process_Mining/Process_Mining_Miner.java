package com.company.Process_Mining;

import com.company.Exceptions.PM_Instance_Relation_Error;
import com.company.Process_Mining.Base_Data.*;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static com.company.Process_Mining.Relation_Type.*;

//Mainclass of the Process Mining,
public class Process_Mining_Miner {

    private final Process_Mining_Settings Mining_Settings;
    private Process_Mining_JSON_Read Reader;
    private List<List<List<Mining_User>>> Used_Users_Filtered_List;
    private List<List<List<Mining_Resource_Count>>> Used_Resource_Count_Filtered_List;
    private List<List<Mining_Activity>> Used_Activity_Filtered_List;
    HashMap<Mining_User, HashMap<Mining_User, Relation_Count>> User_Relation_Hashmap;
    private List<Mining_Activity> Start_Activities;
    private Relation_Places Final_Place;
    private HashMap<Integer, List<LocalTime>> Delay_per_Instance_On_Activity;
    private HashMap<Integer, List<LocalTime>> Workingtime_per_Instance_On_Activity;
    private HashMap<Integer, List<LocalTime>> Completetime_per_Instance_On_Activity;
    private HashMap<Mining_Instance, Log_Relation_Data> Complete_Log;
    private HashMap<Mining_Activity, Complete_Time_Activity> Time_Log_By_Activity;
    private List<Transition_Relation> All_Places;
    private Integer Total_User_Relation_Count;
    private HashMap<Integer, Mining_Activity> Activity_Hashmap;
    private Mining_Logger Print_To_File;
    private HashMap<Mining_Resource, List<Timed_Resource_Usage_By_Activity>> Timed_Mining_Activity_By_Resource;
    private HashMap<Mining_User, List<Timed_User_Usage_By_Activity>> Timed_Mining_Activity_per_User;

    public Process_Mining_Miner(Process_Mining_JSON_Read Reader, Process_Mining_Settings Settings) {
        Used_Users_Filtered_List = new ArrayList<>();
        this.Reader = Reader;
        Used_Resource_Count_Filtered_List = new ArrayList<>();
        Used_Activity_Filtered_List = new ArrayList<>();
        Mining_Settings = Settings;
        Complete_Log = new HashMap<>();
        Time_Log_By_Activity = new HashMap<>();
        User_Relation_Hashmap = new HashMap<>();
        Total_User_Relation_Count = 0;
        Start_Activities = new ArrayList<>();
        Final_Place = new Relation_Places();
        Delay_per_Instance_On_Activity = new HashMap<>();
        Workingtime_per_Instance_On_Activity = new HashMap<>();
        Completetime_per_Instance_On_Activity = new HashMap<>();
        All_Places = new ArrayList<>();
        Activity_Hashmap = new HashMap<>();
        Timed_Mining_Activity_per_User = new HashMap<>();
    }

    public Process_Mining_JSON_Read getReader() {
        return Reader;
    }

    public void setReader(Process_Mining_JSON_Read reader) {
        Reader = reader;
    }

    public void start_Mining() {
        System.out.println("Starting Filter");
        FilterLog();
    }


    private void FilterLog() {
        FilterByActivity();
        Initialize_Activity_Time_Map(Delay_per_Instance_On_Activity);
        Initialize_Activity_Time_Map(Workingtime_per_Instance_On_Activity);
        Initialize_Activity_Time_Map(Completetime_per_Instance_On_Activity);
        FilterByActivityWorkingTime();
        //FilterByUserWorkingTime(); Obsolete
        //FilterByResourceWorkingTime(); Obsolete
        generate_Extended_Alpha_Mapping();
        //Mining Logger prints the extended analysis into several Files for further Research in Python or Excel
        Print_To_File = new Mining_Logger(Activity_Hashmap, User_Relation_Hashmap, Total_User_Relation_Count, Timed_Mining_Activity_per_User, Timed_Mining_Activity_By_Resource, Time_Log_By_Activity, All_Places);
        Print_To_File.LogMining();
    }

    private void generate_Extended_Alpha_Mapping() {

        //Used for Relations Related and followed by.
        HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap = new HashMap<>();
        //Used for Relations 2-Step-Sequence
        HashMap<Integer, HashMap<Integer, Relation_Count>> Sequence_Hashmap = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Relation_Count>> Parallel_Hashmap = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Relation_Count>> One_Loop_Map = new HashMap<>();

        //Initialize all Hashmaps based on the Activities found in the Log (i.e. every Hashmap is a table mapping
        //each Activity to each activity in the form of single Cells of the Table) and for the Userrelation Hashmap
        //and the Resource Usage Hashmap
        Initialize_Activity_HashMap();
        Initialize_Activity_Relation_Hashmap(Relation_Hashmap);
        Initialize_Activity_Relation_Hashmap(Sequence_Hashmap);
        Initialize_Activity_Relation_Hashmap(Parallel_Hashmap);
        Initialize_Resource_Usage_Map();
        Initialize_User_Relation_Map(User_Relation_Hashmap);

        //Checks for Activity Relation in the Caselist i.e. it generates a Process Tree for each Case
        //in the Form that there are two Hashmaps in the End. One which maps each Mining_instance to its Pairs
        //(i.e. Begining, Working, Ending a Event) and one wich Maps every started Mining_instance to their directly
        //followed Mining_Instances (i.e. those that Begin in the same Second as those that finish)
        //right now it only does this based on Mining_Instances which are Generated for Logevents for EPK-Functions.
        for (List<Mining_Instance> Single_Mining_Instance : Reader.getSorted_Instance_List()) {
            check_For_Activity_Relation(Relation_Hashmap, Single_Mining_Instance);
        }
        //Calculates User Relation based on the Processtree discovered.
        Calculate_User_Relations();
        //Calculate Userusage on a Activity and ResourceUsage
        Calculate_User_At_ActivityMap();
        Calculate_Resource_Usage();
        //Calculates the Complete Delay and Workingtime for each distinct Activity in the Log
        Calculate_Times_At_Activities();
        //Process Tree discovery of Relation between Activities
        generate_enhanced_Dependency(Relation_Hashmap);
        //Generates Final_Place (i.e it searches for the Last Activities that are used and
        // generates a Place as a successor for them
        generate_Final_Places(Relation_Hashmap);
        //Identify 1-Step Loops based on the Process Tree Discovery of Relations and Marks their ID??s
        List<Integer> Mark_For_One_Loops = Identify_One_Step_Loops(Relation_Hashmap);
        //Delete Loops from the Hashmaps and re-relate all Incoming Relations of the Loops to the outgoing Relations of them.
        Preprocess_One_Loops(Relation_Hashmap, Mark_For_One_Loops, One_Loop_Map);
        //Identify 2-step Loops based on the Process Trees of Cases
        Identify_Two_Step_Loops(Relation_Hashmap, Sequence_Hashmap);
        //calculate the Followed By Relation based on the Process Tree Discovered Related and Bisequence Relation
        generate_Direct_Dependency(Relation_Hashmap, Sequence_Hashmap);
        //calculate Parallelism based on the Process Tree Discovered Related and Bisequence Relation
        generate_Activity_Parallel_Execution(Relation_Hashmap, Sequence_Hashmap, Parallel_Hashmap);
        List<Relation_Places> Min_Set = new ArrayList<>();
        //Step 4 of the Basic Alpha Algorithm on the Relations without 1-Step-Loop Activities.
        generate_Min_Places(Min_Set, Relation_Hashmap);
        //Just to be sure
        Clean_Min_Places(Min_Set, Relation_Hashmap);
        //Step 5 of the Basic Alpha ALgorithm
        List<Relation_Places> Max_Set = generate_max_Set(Min_Set, Parallel_Hashmap, Relation_Hashmap);
        //The Previous Steps 4 and 5 implicitly generate Places generated in Step 6 of Alpha

        //Generate Transitions based on the Activities in the Log and connect them to the Places calculated by Alpha
        //(i.e. Step 7)
        List<Transition_Relation> Transition_Relations = getTransition_Relations(Max_Set);
        //Postprocess Phase, Calculate Possible Loop Places and the according Transitions for the deleted 1-Loop Activities
        List<Transition_Relation> possibleLoopTransitions = getPossibleLoopTransition_Relations(Mark_For_One_Loops, Max_Set);
        //Based on the Possible Transition/Place Relations, calculate the exact needed Places for those Activities
        List<Transition_Relation> LoopPlaces = PostProcess_One_Loops(possibleLoopTransitions, One_Loop_Map, Mark_For_One_Loops);
        //Add All Loop Places to the Transition Relations, and calculate Transition Relations for the Start and Final Place.
        Transition_Relations.addAll(LoopPlaces);
        Transition_Relations.addAll(GenerateTransactional_Relation_Final());
        Transition_Relations.addAll(GenerateTransactional_Relation_Start());
        All_Places = Transition_Relations;
    }

    private void Initialize_Activity_Relation_Hashmap(HashMap<Integer, HashMap<Integer, Relation_Count>> relation_hashmap) {
        List<Mining_Activity> ActivityList = Reader.getActivityList();
        for (Mining_Activity Y_Activity : ActivityList) {
            if (!relation_hashmap.containsKey(Y_Activity.getNode_ID())) {
                HashMap<Integer, Relation_Count> newRow = new HashMap<>();
                for (Mining_Activity X_Activity : ActivityList) {
                    newRow.put(X_Activity.getNode_ID(), new Relation_Count());
                }
                relation_hashmap.put(Y_Activity.getNode_ID(), newRow);
            }
        }
    }

    private void Initialize_Activity_Time_Map(HashMap<Integer, List<LocalTime>> to_create) {
        List<Mining_Activity> Activity_List = Reader.getActivityList();
        for (Mining_Activity Activity : Activity_List) {
            to_create.put(Activity.getNode_ID(), new ArrayList<>());
        }
    }

    private void Initialize_Resource_Usage_Map() {
        List<Mining_Resource> All_Resources = Reader.getResourceList();
        Timed_Mining_Activity_By_Resource = new HashMap<>();
        for (Mining_Resource Resource : All_Resources) {
            Timed_Mining_Activity_By_Resource.put(Resource, new ArrayList<>());
        }
    }

    private void Initialize_User_Relation_Map(HashMap<Mining_User, HashMap<Mining_User, Relation_Count>> user_relation_hashmap) {
        {
            List<Mining_User> UserList = Reader.getUserlist();
            for (Mining_User Y_User : UserList) {
                if (!user_relation_hashmap.containsKey(Y_User.getP_ID())) {
                    HashMap<Mining_User, Relation_Count> newRow = new HashMap<>();
                    for (Mining_User X_User : UserList) {
                        newRow.put(X_User, new Relation_Count());
                    }
                    user_relation_hashmap.put(Y_User, newRow);
                }
            }
        }
    }

    //Searches for Mining_Instance Objects, which Used a User. inserts on a Map for this User a List of All Mining_Instance Object which
    // Used that User i.e this Method calculates a Hashmap wich maps a User to all its occurences in Mining_Instances.
    private void Calculate_User_At_ActivityMap() {
        for (Map.Entry<Mining_Instance, Log_Relation_Data> Complete_Instance_Log : Complete_Log.entrySet()) {
            Log_Relation_Data Log = Complete_Instance_Log.getValue();
            for (Map.Entry<Mining_Instance, List<Mining_Instance>> Single_Instance_Log : Log.getSingle_Mining_Instance_Map().entrySet()) {
                if (Single_Instance_Log.getValue().size() == 2) {
                    if (!Single_Instance_Log.getValue().get(1).getUsed_Users().isEmpty()) {
                        Mining_Instance Working = Single_Instance_Log.getValue().get(1);
                        for (Mining_User User : Working.getUsed_Users()) {
                            if (Timed_Mining_Activity_per_User.containsKey(User)) {
                                Timed_Mining_Activity_per_User.get(User).add(new Timed_User_Usage_By_Activity(Working.getActivity(), Working.getActivity_Day(), Working.getDuration(), false));
                            } else {
                                List<Timed_User_Usage_By_Activity> Timed_activities = new ArrayList<>();
                                Timed_activities.add(new Timed_User_Usage_By_Activity(Working.getActivity(), Working.getActivity_Day(), Working.getDuration(), false));
                                Timed_Mining_Activity_per_User.put(User, Timed_activities);
                            }
                        }
                    }
                }
                if (Single_Instance_Log.getValue().size() == 3) {

                    Mining_Instance Working = Single_Instance_Log.getValue().get(1);
                    for (Mining_User User : Working.getUsed_Users()) {
                        if (Timed_Mining_Activity_per_User.containsKey(User)) {
                            Timed_Mining_Activity_per_User.get(User).add(new Timed_User_Usage_By_Activity(Working.getActivity(), Working.getActivity_Day(), Working.getDuration(), false));
                        } else {
                            List<Timed_User_Usage_By_Activity> Timed_activities = new ArrayList<>();
                            Timed_activities.add(new Timed_User_Usage_By_Activity(Working.getActivity(), Working.getActivity_Day(), Working.getDuration(), false));
                            Timed_Mining_Activity_per_User.put(User, Timed_activities);
                        }
                    }

                    Mining_Instance Finishing = Single_Instance_Log.getValue().get(2);
                    for (Mining_User User : Finishing.getUsed_Users()) {
                        if (Timed_Mining_Activity_per_User.containsKey(User)) {
                            Timed_Mining_Activity_per_User.get(User).add(new Timed_User_Usage_By_Activity(Finishing.getActivity(), Finishing.getActivity_Day(), Finishing.getDuration(), true));
                        } else {
                            List<Timed_User_Usage_By_Activity> Timed_activities = new ArrayList();
                            Timed_activities.add(new Timed_User_Usage_By_Activity(Finishing.getActivity(), Finishing.getActivity_Day(), Finishing.getDuration(), true));
                            Timed_Mining_Activity_per_User.put(User, Timed_activities);
                        }
                    }
                }
            }
        }


    }

    private List<Transition_Relation> GenerateTransactional_Relation_Start() {
        Relation_Places Start = new Relation_Places();
        List<Integer> Start_IDs = new ArrayList<>();
        Start.setStart(true);
        for (Mining_Activity Start_Activity : Start_Activities) {
            if (!Start.getTo().contains(Start_Activity)) {
                Start_IDs.add(Start_Activity.getNode_ID());
                Start.getTo().add(Start_Activity);
            }
        }
        List<Transition_Relation> Resultlist = new ArrayList<>();
        for (Integer Start_ID : Start_IDs) {
            Transition_Relation newStartRelation = new Transition_Relation(Start_ID, Start, false);
            Resultlist.add(newStartRelation);
        }
        return Resultlist;
    }

    private List<Transition_Relation> GenerateTransactional_Relation_Final() {
        List<Integer> Final_IDs = new ArrayList<>();
        for (Mining_Activity Final_Activity : Final_Place.getFrom()) {
            if (!Final_IDs.contains(Final_Activity.getNode_ID())) {
                Final_IDs.add(Final_Activity.getNode_ID());
            }
        }
        List<Transition_Relation> Resultlist = new ArrayList<>();
        for (Integer Final_ID : Final_IDs) {
            Transition_Relation FinalRelation = new Transition_Relation(Final_ID, Final_Place, true);
            Resultlist.add(FinalRelation);
        }
        return Resultlist;
    }

    private void Initialize_Activity_HashMap() {

        List<Mining_Activity> All_Activities = Reader.getActivityList();
        for (Mining_Activity Activity : All_Activities) {
            if (!Activity_Hashmap.containsKey(Activity.getNode_ID())) {
                Activity_Hashmap.put(Activity.getNode_ID(), Activity);
            } else {
                System.out.println("Error puting new Activity Key to ActivityHashmap (LoopTransition.java)");
            }
        }

    }

    //Generates all possible Transitions/Place Combination of Loop Transitions, based on the Max_Set Places by Alpha.
    private List<Transition_Relation> getPossibleLoopTransition_Relations(List<Integer> mark_for_one_loops,
                                                                          List<Relation_Places> max_set) {
        List<Transition_Relation> Resultlist = new ArrayList<>();
        for (Integer ID : mark_for_one_loops) {
            for (Relation_Places place : max_set) {
                Transition_Relation newLeftRelation = new Transition_Relation(ID, place, true);
                Transition_Relation newRightRelation = new Transition_Relation(ID, place, false);
                Resultlist.add(newLeftRelation);
                Resultlist.add(newRightRelation);
            }
        }

        List<Transition_Relation> Mark_For_Deletion = new ArrayList<>();
        for (Transition_Relation first : Resultlist) {
            for (Transition_Relation second : Resultlist) {
                if (!first.equals(second) && !Mark_For_Deletion.contains(first) && !Mark_For_Deletion.contains(second) && first.isSame(second)) {
                    Mark_For_Deletion.add(second);
                }
            }
        }
        Resultlist.removeAll(Mark_For_Deletion);
        return Resultlist;
    }

    private List<Transition_Relation> getTransition_Relations(List<Relation_Places> max_set) {
        List<Transition_Relation> Resultlist = new ArrayList<>();
        for (Relation_Places Place : max_set) {
            Resultlist.addAll(generateLeftTransaction(Place));
            Resultlist.addAll(generateRightTransaction(Place));
        }

        //Clean Transactional_Relation_Place From Double Entries

        List<Transition_Relation> Mark_For_Deletion = new ArrayList<>();
        for (Transition_Relation first : Resultlist) {
            for (Transition_Relation second : Resultlist) {
                if (!first.equals(second) && !Mark_For_Deletion.contains(first) && !Mark_For_Deletion.contains(second) && first.isSame(second)) {
                    Mark_For_Deletion.add(second);
                }
            }
        }
        Resultlist.removeAll(Mark_For_Deletion);
        return Resultlist;
    }

    private List<Transition_Relation> generateLeftTransaction(Relation_Places place) {
        List<Transition_Relation> leftList = new ArrayList<>();
        for (Mining_Activity Activity : place.getFrom()) {
            Transition_Relation newRelation = new Transition_Relation(Activity.getNode_ID(), place, true);
            leftList.add(newRelation);
        }
        return leftList;
    }

    private List<Transition_Relation> generateRightTransaction(Relation_Places place) {
        List<Transition_Relation> rightList = new ArrayList<>();
        for (Mining_Activity Activity : place.getTo()) {
            Transition_Relation newRelation = new Transition_Relation(Activity.getNode_ID(), place, false);
            rightList.add(newRelation);
        }
        return rightList;
    }


    private void Calculate_Times_At_Activities() {
        for (Map.Entry<Mining_Instance, Log_Relation_Data> Log_Instance : Complete_Log.entrySet()) {
            HashMap<Mining_Instance, List<Mining_Instance>> Instance_Log = Log_Instance.getValue().getSingle_Mining_Instance_Map();
            for (Map.Entry<Mining_Instance, List<Mining_Instance>> Single_Instance_Log : Instance_Log.entrySet()) {
                List<Mining_Instance> Complete_Instance_At_Activity = Single_Instance_Log.getValue();
                if (!Time_Log_By_Activity.containsKey(Complete_Instance_At_Activity.get(0).getActivity())) {
                    Time_Log_By_Activity.put(Complete_Instance_At_Activity.get(0).getActivity(), new Complete_Time_Activity());
                }
                Complete_Time_Activity Time_At_Activity = Time_Log_By_Activity.get(Complete_Instance_At_Activity.get(0).getActivity());
                List<Time_Log_Data> Time_Log_List_By_Instance = Time_At_Activity.getSingle_Instance_Activity_Time();
                Integer Instance_ID = Complete_Instance_At_Activity.get(0).getInstance_ID();
                Duration Delay_To_Start = Duration.ZERO;
                Duration Working_Time = Duration.ZERO;
                if (Complete_Instance_At_Activity.size() == 3) {
                    Mining_Instance Schedule = Complete_Instance_At_Activity.get(0);
                    Mining_Instance Working = Complete_Instance_At_Activity.get(1);
                    Mining_Instance Finished = Complete_Instance_At_Activity.get(2);
                    int ScheduleDay = Schedule.getActivity_Day();
                    int WorkingDay = Working.getActivity_Day();
                    int FinishedDay = Finished.getActivity_Day();
                    Delay_To_Start = Duration.between(Schedule.getDuration(), Working.getDuration());
                    Working_Time = Duration.between(Working.getDuration(), Finished.getDuration());
                    if (ScheduleDay < WorkingDay) {
                        for (int i = ScheduleDay; i < WorkingDay; i++) {
                            Delay_To_Start = Delay_To_Start.plus(Duration.ofHours(24));
                        }
                    }
                    if (WorkingDay < FinishedDay) {
                        for (int i = WorkingDay; i < FinishedDay; i++) {
                            Working_Time = Working_Time.plus(Duration.ofHours(24));
                        }
                    }
                    Duration CompleteTime = Delay_To_Start.plus(Working_Time);
                    Time_At_Activity.setComplete_Delay(Time_At_Activity.getComplete_Delay().plus(Delay_To_Start));
                    Time_At_Activity.setComplete_WorkingTime(Time_At_Activity.getComplete_WorkingTime().plus(Working_Time));
                    Time_At_Activity.setComplete_Time(Time_At_Activity.getComplete_Time().plus(CompleteTime));
                } else if (Complete_Instance_At_Activity.size() == 2) {
                    Mining_Instance Schedule = Complete_Instance_At_Activity.get(0);
                    Mining_Instance Working = Complete_Instance_At_Activity.get(1);
                    int ScheduleDay = Schedule.getActivity_Day();
                    int WorkingDay = Working.getActivity_Day();
                    Delay_To_Start = Duration.between(Schedule.getDuration(), Working.getDuration());
                    if (ScheduleDay < WorkingDay) {
                        for (int i = ScheduleDay; i < WorkingDay; i++) {
                            Delay_To_Start = Delay_To_Start.plus(Duration.ofHours(24));
                        }
                    }
                    Time_At_Activity.setComplete_Delay(Time_At_Activity.getComplete_Delay().plus(Delay_To_Start));
                    Time_At_Activity.setComplete_Time(Time_At_Activity.getComplete_Time().plus(Delay_To_Start));
                }
                Time_Log_List_By_Instance.add(new Time_Log_Data(Instance_ID, Delay_To_Start, Working_Time));
            }
        }
    }

    //Searches for Mining_Instance Objects, which Used a Resource. inserts on a Map for this Resource a List of All Mining_Instance Object which
    // Used that Resource i.e this Method calculates a Hashmap wich maps a Resource to all its occurences in Mining_Instances.
    private void Calculate_Resource_Usage() {
        for (Map.Entry<Mining_Instance, Log_Relation_Data> Instance_Log : Complete_Log.entrySet()) {
            HashMap<Mining_Instance, List<Mining_Instance>> Single_Instance_Activity_Map = Instance_Log.getValue().getSingle_Mining_Instance_Map();
            for (Map.Entry<Mining_Instance, List<Mining_Instance>> Single_Instance_Activity : Single_Instance_Activity_Map.entrySet()) {
                List<Mining_Instance> LifecycleList = Single_Instance_Activity.getValue();
                if (!LifecycleList.isEmpty() && LifecycleList.size() > 1) {
                    List<Mining_Resource_Count> ResourceList = LifecycleList.get(1).getUsed_Resources();
                    for (Mining_Resource_Count Used_Resource : ResourceList) {
                        Timed_Resource_Usage_By_Activity new_Resource_Event = new Timed_Resource_Usage_By_Activity(LifecycleList.get(1).getActivity(), LifecycleList.get(1).getActivity_Day(), LifecycleList.get(1).getDuration(), Used_Resource.getCount());
                        Timed_Mining_Activity_By_Resource.get(Used_Resource.getResource()).add(new_Resource_Event);
                    }
                }
                if (!LifecycleList.isEmpty() && LifecycleList.size() > 2) {
                    List<Mining_Resource_Count> ResourceList = LifecycleList.get(2).getUsed_Resources();
                    for (Mining_Resource_Count Used_Resource : ResourceList) {
                        Timed_Resource_Usage_By_Activity new_Resource_Event = new Timed_Resource_Usage_By_Activity(LifecycleList.get(2).getActivity(), LifecycleList.get(2).getActivity_Day(), LifecycleList.get(2).getDuration(), Used_Resource.getCount());
                        Timed_Mining_Activity_By_Resource.get(Used_Resource.getResource()).add(new_Resource_Event);
                    }
                }
            }
        }
    }

    //traverses the Process trees and Checks if for each distinct activity, and its successors, Users where used.
    //if yes, it adds a Relation into the User_Relation_Hashmap which relates each User of the Activity to the Users of the Successor.
    //Also each Relation is Counted to get additional information about how often the communication between to two Users happens.
    private void Calculate_User_Relations() {
        for (Map.Entry<Mining_Instance, Log_Relation_Data> Instance_Log : Complete_Log.entrySet()) {
            HashMap<Mining_Instance, List<Mining_Instance>> Instance_Relation_Log = Instance_Log.getValue().getMining_Instances_Relations();
            HashMap<Mining_Instance, List<Mining_Instance>> Single_Instance_Activity_Map = Instance_Log.getValue().getSingle_Mining_Instance_Map();

            for (Map.Entry<Mining_Instance, List<Mining_Instance>> Single_Instance_Relation : Instance_Relation_Log.entrySet()) {
                List<Mining_Instance> Single_Activity_Log = Single_Instance_Activity_Map.get(Single_Instance_Relation.getKey());
                if (Single_Activity_Log.size() > 1 && !Single_Activity_Log.get(1).getUsed_Users().isEmpty()) {
                    List<Mining_User> Users_From = Single_Activity_Log.get(1).getUsed_Users();
                    List<Mining_Instance> Instances_Related_To = Single_Instance_Relation.getValue();
                    for (Mining_Instance Related_To : Instances_Related_To) {
                        if (Single_Instance_Activity_Map.get(Related_To).size() > 1) {
                            List<Mining_User> Users_To = Single_Instance_Activity_Map.get(Related_To).get(1).getUsed_Users();
                            if (!Users_To.isEmpty()) {
                                for (Mining_User User_From : Users_From) {
                                    for (Mining_User User_To : Users_To) {
                                        if (!User_From.equals(User_To)) {
                                            //TODO OPTIONAL: IF User Self Reference is allowed, delete upper if -> will result in Transitions from User 1 to User 1
                                            if (User_Relation_Hashmap.containsKey(User_From)) {
                                                Total_User_Relation_Count++;
                                                Integer Rel_Count = User_Relation_Hashmap.get(User_From).get(User_To).getCount() + 1;
                                                User_Relation_Hashmap.get(User_From).replace(User_To, new Relation_Count(Related, Rel_Count));
                                            } else {
                                                System.out.println("User Error in Calculate-User_relation, User From not Found");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (Users_From.size() > 1) {
                        for (Mining_User User_From : Users_From) {
                            for (Mining_User User_To : Users_From) {
                                if (!User_From.equals(User_To)) {
                                    //TODO Same as Above
                                    Total_User_Relation_Count++;
                                    Integer Rel_Count = User_Relation_Hashmap.get(User_From).get(User_To).getCount() + 1;
                                    User_Relation_Hashmap.get(User_From).replace(User_To, new Relation_Count(Related, Rel_Count));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //Helper Method to Delete Places which are a direct Copy of Another.
    private void Clean_Min_Places(List<Relation_Places> min_set, HashMap<Integer, HashMap<Integer, Relation_Count>> relation_hashmap) {
        List<Relation_Places> Mark_For_Deletion = new ArrayList<>();
        for (Relation_Places relation : min_set) {
            boolean delete_relation = false;
            for (Mining_Activity from_Activity_A : relation.getFrom()) {
                if (!delete_relation) {
                    for (Mining_Activity from_Activity_B : relation.getFrom()) {
                        if (relation_hashmap.get(from_Activity_A.getNode_ID()).get(from_Activity_B.getNode_ID()).getRelation_type() != Relation_Type.None) {
                            Mark_For_Deletion.add(relation);
                            delete_relation = true;
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            if (!delete_relation) {
                for (Mining_Activity to_Activity_A : relation.getTo()) {
                    if (!delete_relation) {
                        for (Mining_Activity to_Activity_B : relation.getTo()) {
                            if (relation_hashmap.get(to_Activity_A.getNode_ID()).get(to_Activity_B.getNode_ID()).getRelation_type() != Relation_Type.None) {
                                Mark_For_Deletion.add(relation);
                                delete_relation = true;
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        if (!Mark_For_Deletion.isEmpty()) {
            min_set.removeAll(Mark_For_Deletion);
        }
    }

    //Alpha-Algorithm Method to generate a Set of Minimal Places (i.e. the Step 4 of the ALpha-Algorithm)
    //It Generates a Relation_Place Object for each Followed By Relation, with the Source Activity in a From-list and the sink Activity
    //in a To-List.
    private void generate_Min_Places(List<Relation_Places> min_set, HashMap<Integer, HashMap<Integer, Relation_Count>> relation_Hashmap) {
        HashMap<Integer, Mining_Activity> All_Activities = new HashMap<>();
        for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_By_Single_Elem : relation_Hashmap.entrySet()) {
            for (Map.Entry<Integer, Relation_Count> Relation_From_Single_To_Single_Elem : Relation_By_Single_Elem.getValue().entrySet()) {
                if (Relation_From_Single_To_Single_Elem.getValue().getRelation_type().equals(Relation_Type.Followed_by)) {
                    Mining_Activity From = All_Activities.get(Relation_By_Single_Elem.getKey());
                    if (From == null) {
                        List<Mining_Activity> All_Activities_List = Reader.getActivityList();
                        for (Mining_Activity Activity : All_Activities_List) {
                            if (Activity.getNode_ID() == Relation_By_Single_Elem.getKey()) {
                                All_Activities.put(Activity.getNode_ID(), Activity);
                                From = Activity;
                                break;
                            }
                        }
                    }
                    Mining_Activity To = All_Activities.get(Relation_From_Single_To_Single_Elem.getKey());
                    if (To == null) {
                        List<Mining_Activity> All_Activities_List = Reader.getActivityList();
                        for (Mining_Activity Activity : All_Activities_List) {
                            if (Activity.getNode_ID() == Relation_From_Single_To_Single_Elem.getKey()) {
                                All_Activities.put(Activity.getNode_ID(), Activity);
                                To = Activity;
                                break;
                            }
                        }
                    }
                    Relation_Places new_Min_Place = new Relation_Places();
                    new_Min_Place.getFrom().add(From);
                    new_Min_Place.getTo().add(To);
                    min_set.add(new_Min_Place);
                }
            }
        }
    }


    //Alpha-Algorithm Step 5, generates a overarching Set of Places, based on the min_Set, which fuse Places of the Minimal Places together.
    //For Each overarching Place, all Distinct Activities of the From and To, Part have to be unrelated to each other.
    private List<Relation_Places> generate_max_Set(List<Relation_Places> min_Set,
                                                   HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap,
                                                   HashMap<Integer, HashMap<Integer, Relation_Count>> Parallel_Hashmap) {
        List<Relation_Places> ResultList = new ArrayList<>(min_Set);
        List<Relation_Places> Mark_For_Deletion = new ArrayList<>();
        List<Relation_Places> Working_List = new ArrayList<>();

        boolean done = false;
        while (!done) {
            boolean has_right = false;
            boolean has_left = false;

            ListIterator<Relation_Places> first_Iterator_Right = ResultList.listIterator();
            while (first_Iterator_Right.hasNext()) {
                Relation_Places first_Relation_Place = first_Iterator_Right.next();
                ListIterator<Relation_Places> second_iterator_right = ResultList.listIterator();
                while (second_iterator_right.hasNext()) {
                    Relation_Places second_Relation_Place = second_iterator_right.next();
                    if (first_Relation_Place.has_Same_to(second_Relation_Place) && !first_Relation_Place.equals(second_Relation_Place)) {
                        Relation_Places biggerSet = new Relation_Places();
                        biggerSet.getFrom().addAll(first_Relation_Place.getFrom());
                        for (Mining_Activity to_Add : second_Relation_Place.getFrom()) {
                            if (!biggerSet.getFrom().contains(to_Add)) {
                                biggerSet.getFrom().add(to_Add);
                            }
                        }
                        if (check_for_Relation_in_table(biggerSet.getFrom(), Parallel_Hashmap, Relation_Hashmap)) {
                            has_right = true;
                            biggerSet.getTo().addAll(first_Relation_Place.getTo());
                            Mark_For_Deletion.add(first_Relation_Place);
                            Mark_For_Deletion.add(second_Relation_Place);
                            Working_List.add(biggerSet);
                        }
                    }
                }
            }

            ListIterator<Relation_Places> first_iterator_left = ResultList.listIterator();
            while (first_iterator_left.hasNext()) {
                Relation_Places first_Relation_Place = first_iterator_left.next();
                ListIterator<Relation_Places> second_iterator_left = ResultList.listIterator();
                while (second_iterator_left.hasNext()) {
                    Relation_Places second_Relation_Place = second_iterator_left.next();
                    if (!first_Relation_Place.equals(second_Relation_Place) && first_Relation_Place.has_Same_From(second_Relation_Place)) {

                        Relation_Places biggerSet = new Relation_Places();
                        biggerSet.getTo().addAll(first_Relation_Place.getTo());
                        for (Mining_Activity to_Add : second_Relation_Place.getTo()) {
                            if (!biggerSet.getTo().contains(to_Add)) {
                                biggerSet.getTo().add(to_Add);
                            }
                        }

                        if (check_for_Relation_in_table(biggerSet.getTo(), Parallel_Hashmap, Relation_Hashmap)) {
                            has_left = true;
                            biggerSet.getFrom().addAll(first_Relation_Place.getFrom());
                            Mark_For_Deletion.add(first_Relation_Place);
                            Mark_For_Deletion.add(second_Relation_Place);
                            Working_List.add(biggerSet);
                        }
                    }
                }
            }

            if (!has_right && !has_left) {
                done = true;
            } else {
                Clean_Up_Place_List(Working_List);
                ResultList.removeAll(Mark_For_Deletion);
                ResultList.addAll(Working_List);
                done = false;
            }
        }
        Clean_Up_Place_List(ResultList);
        return ResultList;
    }


    private void generate_Final_Places(HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap) {
        List<Mining_Activity> Final_Activities = new ArrayList<>();
        for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_By_Single_Elem : Relation_Hashmap.entrySet()) {
            boolean isEnding = true;
            for (Map.Entry<Integer, Relation_Count> Relation_From_Single_To_Single_Elem : Relation_By_Single_Elem.getValue().entrySet()) {
                if (Relation_From_Single_To_Single_Elem.getValue().getRelation_type() != None) {
                    isEnding = false;
                    break;
                }
            }
            if (isEnding) {
                Integer Final_ID = Relation_By_Single_Elem.getKey();
                for (Mining_Activity Final : Reader.getActivityList()) {
                    if (Final.getNode_ID() == Final_ID) {
                        Final_Activities.add(Final);
                    }
                }
            }
        }
        Relation_Places newFinal = new Relation_Places();
        newFinal.setFinal(true);
        for (Mining_Activity Final_Activity : Final_Activities) {
            newFinal.getFrom().add(Final_Activity);

        }
        Final_Place = newFinal;
    }

    //Generates Relations through the relation log files in Complete Log
    private void generate_enhanced_Dependency(HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap) {

        for (Map.Entry<Mining_Instance, Log_Relation_Data> Instance_Log : Complete_Log.entrySet()) {
            HashMap<Mining_Instance, List<Mining_Instance>> Instance_Relation_Map = Instance_Log.getValue().getMining_Instances_Relations();
            for (Map.Entry<Mining_Instance, List<Mining_Instance>> Single_Relation : Instance_Relation_Map.entrySet()) {
                for (Mining_Instance Related_To : Single_Relation.getValue()) {
                    if (Relation_Hashmap.get(Single_Relation.getKey().getActivity().getNode_ID())
                            .get(Related_To.getActivity().getNode_ID()).getRelation_type() == Relation_Type.None) {
                        Relation_Hashmap.get(Single_Relation.getKey().getActivity().getNode_ID())
                                .get(Related_To.getActivity().getNode_ID()).setRelation_type(Related);
                        Relation_Hashmap.get(Single_Relation.getKey().getActivity().getNode_ID())
                                .get(Related_To.getActivity().getNode_ID()).incrementCount();
                    } else {
                        Relation_Hashmap.get(Single_Relation.getKey().getActivity().getNode_ID())
                                .get(Related_To.getActivity().getNode_ID()).incrementCount();
                    }
                }
                if (Single_Relation.getValue().size() > 1) {
                    for (Mining_Instance Related_Inbetweet_One : Single_Relation.getValue()) {
                        for (Mining_Instance Related_Inbetween_Two : Single_Relation.getValue()) {
                            if (!Related_Inbetweet_One.equals(Related_Inbetween_Two)) {
                                Relation_Hashmap.get(Related_Inbetweet_One.getActivity().getNode_ID())
                                        .get(Related_Inbetween_Two.getActivity().getNode_ID()).setRelation_type(Relation_Type.Related);
                                Relation_Hashmap.get(Related_Inbetweet_One.getActivity().getNode_ID())
                                        .get(Related_Inbetween_Two.getActivity().getNode_ID()).incrementCount();
                            }
                        }
                    }
                }
            }
        }
    }


    //Calculates the Followed By Relation for Each Activity Set if they are Related in one way, not in both ways,
    //or if they are Related and in a Bisequence
    private void generate_Direct_Dependency(HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap,
                                            HashMap<Integer, HashMap<Integer, Relation_Count>> Sequence_Hashmap) {

        for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_By_Single_Elem : Relation_Hashmap.entrySet()) {

            for (Map.Entry<Integer, Relation_Count> Relation_From_Single_To_Single_Elem : Relation_By_Single_Elem.getValue().entrySet()) {

                if (Relation_From_Single_To_Single_Elem.getValue().getRelation_type() == Related &&
                        (Relation_Hashmap.get(Relation_From_Single_To_Single_Elem.getKey()).get(Relation_By_Single_Elem.getKey()).getRelation_type() != Related ||
                                Sequence_Hashmap.get(Relation_By_Single_Elem.getKey()).get(Relation_From_Single_To_Single_Elem.getKey()).getRelation_type() == Bisequence)) {
                    Relation_From_Single_To_Single_Elem.getValue().setRelation_type(Relation_Type.Followed_by);
                }
            }
        }
    }

    //Calculates the Parallel  Relation for Each Activity Set if they are Related in both ways, not in one ways,
    //and if they are not in a Bisequence
    private void generate_Activity_Parallel_Execution(HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap,
                                                      HashMap<Integer, HashMap<Integer, Relation_Count>> Sequence_Hashmap,
                                                      HashMap<Integer, HashMap<Integer, Relation_Count>> Parallel_Hashmap) {

        for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_By_Single_Elem : Relation_Hashmap.entrySet()) {

            for (Map.Entry<Integer, Relation_Count> Relation_From_Single_To_Single_Elem : Relation_By_Single_Elem.getValue().entrySet()) {

                if (Relation_From_Single_To_Single_Elem.getValue().getRelation_type() == Related &&
                        Relation_Hashmap.get(Relation_From_Single_To_Single_Elem.getKey()).get(Relation_By_Single_Elem.getKey()).getRelation_type() == Related &&
                        Sequence_Hashmap.get(Relation_By_Single_Elem.getKey()).get(Relation_From_Single_To_Single_Elem.getKey()).getRelation_type() != Bisequence) {
                    Parallel_Hashmap.get(Relation_By_Single_Elem.getKey()).get(Relation_From_Single_To_Single_Elem.getKey()).setRelation_type(Relation_Type.Parallel);

                }
            }
        }
    }

    //TODO DELETE FROM LOG! PREPROCESS
    private List<Integer> Identify_One_Step_Loops(HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap) {
        List<Integer> Mark_For_One_Loop = new ArrayList<>();
        for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_From_Single_Element : Relation_Hashmap.entrySet()) {
            for (Map.Entry<Integer, Relation_Count> Relation_To_Single_Element : Relation_From_Single_Element.getValue().entrySet()) {
                if (Relation_From_Single_Element.getKey().equals(Relation_To_Single_Element.getKey()) &&
                        Relation_From_Single_Element.getValue().get(Relation_From_Single_Element.getKey()).getRelation_type() == Related) {
                    Mark_For_One_Loop.add(Relation_From_Single_Element.getKey());
                }
            }
        }
        return Mark_For_One_Loop;
    }

    //Based on the Marked for Loops Integerlist, it Searches the Relations Hashmap for each ID and reconnects all incoming Relations to the outgoing Relations

    private void Preprocess_One_Loops(HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap, List<Integer> Mark_For_One_Loop, HashMap<Integer, HashMap<Integer, Relation_Count>> One_Loop_Map) {


        //Copy the Relations of the Loop into a cached Hashmap for the later re-adding of the Loops.
        for (Integer ID : Mark_For_One_Loop) {
            One_Loop_Map.put(ID, Relation_Hashmap.get(ID));
            Relation_Hashmap.remove(ID);

            //Re-relate each incoming Relation of a 1-Step-Loop Activity to the all Outgoing Relations of that Activity.
            for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_From_Single_Element : Relation_Hashmap.entrySet()) {
                for (Map.Entry<Integer, Relation_Count> Relation_To_Single_Element : Relation_From_Single_Element.getValue().entrySet()) {
                    if (Relation_To_Single_Element.getValue().getRelation_type() == Related && Relation_To_Single_Element.getKey().equals(ID)) {
                        for (Map.Entry<Integer, Relation_Count> Relations_From_Deleted_Elem : One_Loop_Map.get(ID).entrySet()) {
                            if (Relations_From_Deleted_Elem.getValue().getRelation_type() == Related && !Relations_From_Deleted_Elem.getKey().equals(ID)) {
                                Relation_From_Single_Element.getValue().get(Relations_From_Deleted_Elem.getKey()).setRelation_type(Related);
                            }
                        }
                    }
                }
            }

            //Remove the 1-Loop-Activity from the Relation Hashmap
            for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_From_Single_Element : Relation_Hashmap.entrySet()) {
                if (Relation_From_Single_Element.getValue().get(ID).getRelation_type() == Related) {
                    if (One_Loop_Map.containsKey(Relation_From_Single_Element.getKey())) {
                        if (One_Loop_Map.get(Relation_From_Single_Element.getKey()).containsKey(ID)) {
                            One_Loop_Map.get(Relation_From_Single_Element.getKey()).get(ID).setRelation_type(Related);
                            Relation_Hashmap.get(Relation_From_Single_Element.getKey()).remove(ID);
                        } else {
                            One_Loop_Map.get(Relation_From_Single_Element.getKey()).put(ID, Relation_Hashmap.get(Relation_From_Single_Element.getKey()).get(ID));
                            Relation_Hashmap.get(Relation_From_Single_Element.getKey()).remove(ID);
                        }
                    } else {
                        One_Loop_Map.put(Relation_From_Single_Element.getKey(), new HashMap<>());
                        Relation_Count original = Relation_Hashmap.get(Relation_From_Single_Element.getKey()).get(ID);
                        One_Loop_Map.get(Relation_From_Single_Element.getKey()).put(ID, original);
                        Relation_Hashmap.get(Relation_From_Single_Element.getKey()).remove(ID);
                    }
                } else {
                    One_Loop_Map.put(Relation_From_Single_Element.getKey(), new HashMap<>());
                    One_Loop_Map.get(Relation_From_Single_Element.getKey()).put(ID, Relation_Hashmap.get(Relation_From_Single_Element.getKey()).get(ID));
                    Relation_Hashmap.get(Relation_From_Single_Element.getKey()).remove(ID);
                }
            }
        }
    }

    //generate in generateLoopSet() all Possible Transition/Place Relations for 1-Step-Loop Activities, based on the
    //Relations of those Activities (in Contrast to the Max Set Places).
    private List<Transition_Relation> PostProcess_One_Loops(List<Transition_Relation> possibleTransitions, HashMap<Integer, HashMap<Integer, Relation_Count>> one_Loop_Map, List<Integer> mark_For_One_Loops) {
        List<Transition_Relation> ResultingLoopPlaces = new ArrayList<>();
        List<Transition_Relation> possibleLoopPlaces = generateLoopSet(mark_For_One_Loops, one_Loop_Map);

        //Only Chose those Transition/Place Combination that was calculated by the Max_Set Combination AND the Relation Combination
        for (Transition_Relation TransitionPlace : possibleTransitions) {
            for (Transition_Relation LoopPlace : possibleLoopPlaces) {
                if (TransitionPlace.isSame(LoopPlace) && !ResultingLoopPlaces.contains(TransitionPlace)) {
                    ResultingLoopPlaces.add(TransitionPlace);
                }
            }
        }
        return ResultingLoopPlaces;
    }


    //generates all Possible PLaces for Loop Transitions that could be inside the resulting Model.
    private List<Transition_Relation> generateLoopSet(List<Integer> mark_for_one_loops, HashMap<Integer, HashMap<Integer, Relation_Count>> one_loop_map) {

        //For Each Loop Activity generate From and To Relations (i.e.Places)
        List<LoopTransition> allLoopTransitions = new ArrayList<>();
        for (Integer LoopID : mark_for_one_loops) {
            List<Integer> FromRelations = new ArrayList<>();
            List<Integer> ToRelations = new ArrayList<>();

            //Calculate all Source Relations
            HashMap<Integer, Relation_Count> LoopRelatedTo = one_loop_map.get(LoopID);
            for (Map.Entry<Integer, Relation_Count> Related : LoopRelatedTo.entrySet()) {
                if (Related.getValue().getRelation_type() == Relation_Type.Related) {
                    if (!ToRelations.contains(Related.getKey())) {
                        ToRelations.add(Related.getKey());
                    }
                }
            }
            //Calculate all Sink Relations
            for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Related_From : one_loop_map.entrySet()) {
                if (Related_From.getValue().get(LoopID).getRelation_type() == Related) {
                    if (!FromRelations.contains(Related_From.getKey())) {
                        FromRelations.add(Related_From.getKey());
                    }
                }
            }
            //Generate new Loop Transition with From and To Relation.
            LoopTransition newLoopTransition = new LoopTransition(LoopID, FromRelations, ToRelations);
            allLoopTransitions.add(newLoopTransition);
        }
        //Translate Loop Transition Objects to Transitition Relation Objects and calculate all Possible Place combinations
        List<Transition_Relation> ResultingList = new ArrayList<>();
        for (LoopTransition LoopTransition : allLoopTransitions) {
            ResultingList.addAll(LoopTransition.generateMaxPlaceSet(Activity_Hashmap));
        }
        return ResultingList;
    }

    //Works on the Process Tree of all Cases to find 2-Step-Sequences, (i.e. two of the same activities that are "interrupted"
    //by a different Activity)
    private void Identify_Two_Step_Loops(HashMap<Integer, HashMap<Integer, Relation_Count>> relation_Hashmap, HashMap<Integer, HashMap<Integer, Relation_Count>> Sequence_Hashmap) {

        List<Mining_Activity> ActivityList = Reader.getActivityList();
        for (Mining_Activity Y_Activity : ActivityList) {
            if (!relation_Hashmap.containsKey(Y_Activity.getNode_ID())) {
                HashMap<Integer, Relation_Count> newRow = new HashMap<>();
                for (Mining_Activity X_Activity : ActivityList) {
                    newRow.put(X_Activity.getNode_ID(), new Relation_Count());
                }
                relation_Hashmap.put(Y_Activity.getNode_ID(), newRow);
            }
        }

        //Searches for Activities in the Complete Log Map that are 2-step Loops
        //and puts the according Relation on the Sequence Hashmap as soon as those are found as a Unisequence.
        for (Map.Entry<Mining_Instance, Log_Relation_Data> Instance_Log : Complete_Log.entrySet()) {
            HashMap<Mining_Instance, List<Mining_Instance>> Instance_Relation_Map = Instance_Log.getValue().getMining_Instances_Relations();
            for (Map.Entry<Mining_Instance, List<Mining_Instance>> Instance_Relations : Instance_Log.getValue().getMining_Instances_Relations().entrySet()) {
                Mining_Instance Source_Instance = Instance_Relations.getKey();
                List<Mining_Instance> Related_Instances = Instance_Relations.getValue();
                for (Mining_Instance Related : Related_Instances) {
                    List<Mining_Instance> Relations_From_Related_Element = Instance_Relation_Map.get(Related);
                    if (!Relations_From_Related_Element.isEmpty()) {
                        for (Mining_Instance Related_From_Related : Relations_From_Related_Element) {
                            if (Related_From_Related.getActivity().getNode_ID() == Source_Instance.getActivity().getNode_ID()) {
                                Sequence_Hashmap.get(Source_Instance.getActivity().getNode_ID()).get(Related.getActivity().getNode_ID()).setRelation_type(Unisequence);
                            }
                        }
                    }
                }
            }
        }

        //if two activities are both in Unisequence to each other they are in a bisequence Relation which should
        //be reflected in the Sequence Hashmap.
        for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relations_From : Sequence_Hashmap.entrySet()) {
            for (Map.Entry<Integer, Relation_Count> Relations_To : Relations_From.getValue().entrySet()) {
                if ((!Relations_To.getKey().equals(Relations_From.getKey())) && Relations_To.getValue().getRelation_type() == Unisequence) {
                    if (Sequence_Hashmap.get(Relations_To.getKey()).get(Relations_From.getKey()).getRelation_type() == Unisequence) {
                        Relations_To.getValue().setRelation_type(Bisequence);
                        Sequence_Hashmap.get(Relations_To.getKey()).get(Relations_From.getKey()).setRelation_type(Bisequence);
                    }
                }
            }
        }
    }

    //Based on a Process Tree this Method calculates a Tree Structure for each Case, i.e. from a Starting Mining_Instance
    //all followed Instanced and the Relations between them can be filtered out.
    //Both Hashmaps are then put into a Map which Maps the first Mining_Instance of a Case to the corresponding Hashmaps of that Case.
    private void check_For_Activity_Relation(HashMap<Integer, HashMap<Integer, Relation_Count>> relation_hashmap, List<Mining_Instance> single_mining_instance) {

        //Single_Mining_Instance_Map is the Map that will result in a complete Mining_Instance (i.e. Begin, Working and Ending of one single Activity)
        //Mining_Instances_Related is a Hashmap wich maps every started Mining_Instances to its direct successors.
        HashMap<Mining_Instance, List<Mining_Instance>> Single_Mining_Instance_Map = new HashMap<>();
        HashMap<Mining_Instance, List<Mining_Instance>> Mining_Instances_Related = new HashMap<>();
        List<Mining_Instance> Working_List = new ArrayList<>(single_mining_instance);
        List<Integer> to_Work_On = new ArrayList<>();
        to_Work_On.add(0);

        Mining_Activity new_Start = Working_List.get(0).getActivity();
        if (Start_Activities.isEmpty()) {
            Start_Activities.add(new_Start);
        } else {
            boolean found = false;
            for (Mining_Activity Start_Activity : Start_Activities) {
                if (Start_Activity.equals(new_Start)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Start_Activities.add(new_Start);
            }
        }
        while (!to_Work_On.isEmpty()) {
            int index = to_Work_On.get(0);
            Mining_Instance Instance_to_Get_Related_Elements = Working_List.get(to_Work_On.get(0));
            Single_Mining_Instance_Map.put(Instance_to_Get_Related_Elements, new ArrayList<>());
            Single_Mining_Instance_Map.get(Instance_to_Get_Related_Elements).add(Instance_to_Get_Related_Elements);
            Mining_Instances_Related.put(Instance_to_Get_Related_Elements, new ArrayList<>());
            to_Work_On.remove(0);
            ListIterator<Mining_Instance> iter = Working_List.listIterator();
            boolean instance_checked = false;
            while (!instance_checked && iter.hasNext()) {
                if (iter.nextIndex() == index) {
                    //start from index to search for related activities
                    List<Mining_Instance> possible_Relations = new ArrayList<>();
                    iter.next();
                    boolean Relation_Checked = false;
                    boolean Error = false;
                    while (iter.hasNext() && !Relation_Checked) {
                        //search for working and finishing instance
                        Mining_Instance possible_Relation = iter.next();
                        if (possible_Relation.getActivity().equals(Instance_to_Get_Related_Elements.getActivity())
                                && possible_Relation.getActivity_Status().equals("Working")) {
                            Single_Mining_Instance_Map.get(Instance_to_Get_Related_Elements).add(possible_Relation);
                        } else if (possible_Relation.getActivity().equals(Instance_to_Get_Related_Elements.getActivity())
                                && possible_Relation.getActivity_Status().equals("Finished")) {
                            Single_Mining_Instance_Map.get(Instance_to_Get_Related_Elements).add(possible_Relation);
                            try {
                                LocalTime Timestamp = possible_Relation.getDuration();
                                boolean finished_find_Scheduled = false;
                                boolean first_Step = true;
                                while (iter.hasNext() && !finished_find_Scheduled) {
                                    Mining_Instance to_Check_Relation = iter.next();
                                    if (first_Step && to_Check_Relation.getActivity_Status().equals("Finished") && to_Check_Relation.getDuration().equals(Timestamp)) {
                                        throw new PM_Instance_Relation_Error("Instance relations can??t be resolved correctly");
                                    } else {
                                        first_Step = false;
                                        if (to_Check_Relation.getActivity_Status().equals("Finished")) {
                                            finished_find_Scheduled = true;
                                        } else if (to_Check_Relation.getActivity_Status().equals("Scheduled")) {
                                            possible_Relations.add(to_Check_Relation);
                                            Mining_Instances_Related.get(Instance_to_Get_Related_Elements).add(to_Check_Relation);
                                        }
                                    }
                                }
                                Relation_Checked = true;
                            } catch (PM_Instance_Relation_Error e) {
                                Relation_Checked = true;
                                Error = true;
                            }
                        }
                        if (Error) {
                            possible_Relations.clear();
                            //TODO EVTL DELETE COMPLETE INSTANCE
                        }
                    }

                    if (!Error && !possible_Relations.isEmpty()) {
                        for (Mining_Instance Relation : possible_Relations) {
                            to_Work_On.add(Working_List.indexOf(Relation));

                            /*if (relation_hashmap.containsKey(Instance_to_Get_Related_Elements.getActivity().getNode_ID())) {
                                HashMap<Integer, Relation_Count> related_to_Activity_Hashmap
                                        = relation_hashmap.get(Instance_to_Get_Related_Elements.getActivity().getNode_ID());

                                if (related_to_Activity_Hashmap.containsKey(Relation.getActivity().getNode_ID())) {
                                    related_to_Activity_Hashmap.get(Relation.getActivity().getNode_ID()).incrementCount();
                                    related_to_Activity_Hashmap.get(Relation.getActivity().getNode_ID()).setRelation_type(Related);
                                }
                            }*/
                        }
                    }
                    instance_checked = true;
                } else {
                    iter.next();
                }
            }
        }
        Complete_Log.put(Working_List.get(0), new Log_Relation_Data(Single_Mining_Instance_Map, Mining_Instances_Related));
    }

    private void FilterByResource() {
        Mining_Resource_Count Empty_Resource_Count = new Mining_Resource_Count(new Mining_Resource("Empty", 0), 0);
        List<List<Mining_Instance>> sorted_Instance_List = Reader.getSorted_Instance_List();
        List<List<List<Mining_Resource_Count>>> used_Resource_Count_per_Instance = new ArrayList<>();
        for (List<Mining_Instance> single_Instance_List : sorted_Instance_List) {
            List<List<Mining_Resource_Count>> used_Resource_Count_By_Single_Instance = new ArrayList<>();
            for (Mining_Instance single_Activity : single_Instance_List) {
                if (single_Activity.getActivity_Status().equals("Working")) {
                    boolean hasResource = false;
                    List<Mining_Resource_Count> used_Resource_Count_By_Single_Instance_Per_Activity = new ArrayList<>();
                    for (Mining_Resource_Count used_Resource_Count : single_Activity.getUsed_Resources()) {
                        hasResource = true;
                        used_Resource_Count_By_Single_Instance_Per_Activity.add(used_Resource_Count);
                    }
                    if (!hasResource) {
                        used_Resource_Count_By_Single_Instance_Per_Activity.add(Empty_Resource_Count);
                    }
                    used_Resource_Count_By_Single_Instance.add(used_Resource_Count_By_Single_Instance_Per_Activity);
                }
            }
            used_Resource_Count_per_Instance.add(used_Resource_Count_By_Single_Instance);
        }
        Used_Resource_Count_Filtered_List = used_Resource_Count_per_Instance;
    }

    private void FilterByUser() {
        Mining_User Empty_User = new Mining_User("Empty", "Empty", 0);
        List<List<Mining_Instance>> sorted_Instance_List = Reader.getSorted_Instance_List();
        List<List<List<Mining_User>>> used_Users_per_Instance = new ArrayList<>();
        for (List<Mining_Instance> single_Instance_List : sorted_Instance_List) {
            List<List<Mining_User>> used_Users_By_Single_Instance = new ArrayList<>();
            for (Mining_Instance single_Activity : single_Instance_List) {
                if (single_Activity.getActivity_Status().equals("Working")) {
                    boolean hasUser = false;
                    List<Mining_User> used_User_By_Single_Instance_Per_Activity = new ArrayList<>();
                    for (Mining_User used_User : single_Activity.getUsed_Users()) {
                        hasUser = true;
                        used_User_By_Single_Instance_Per_Activity.add(used_User);
                    }
                    if (!hasUser) {
                        used_User_By_Single_Instance_Per_Activity.add(Empty_User);
                    }
                    used_Users_By_Single_Instance.add(used_User_By_Single_Instance_Per_Activity);
                }
            }
            used_Users_per_Instance.add(used_Users_By_Single_Instance);
        }
        Used_Users_Filtered_List = used_Users_per_Instance;
    }

    //POSSIBLE DEAD METHOD
    private void generateMaxRelationSet(HashMap<Integer, HashMap<Integer, Relation_Count>> Relation_Hashmap) {
        List<Relation_Places> Places = new ArrayList<>();
        List<Relation_Places> WorkingList = new ArrayList<>();

        for (Map.Entry<Integer, HashMap<Integer, Relation_Count>> Relation_By_Single_Elem : Relation_Hashmap.entrySet()) {

            for (Map.Entry<Integer, Relation_Count> Relation_From_Single_To_Single_Elem : Relation_By_Single_Elem.getValue().entrySet()) {

                Relation_By_Single_Elem.getValue();
                Relation_From_Single_To_Single_Elem.getValue();
            }
        }
    }

    //HELPER FOR MAX_SET
    private boolean check_for_Relation_in_table(List<Mining_Activity> to_check, HashMap<Integer, HashMap<Integer, Relation_Count>> Parallel_Hashmap, HashMap<Integer, HashMap<Integer, Relation_Count>> relation_hashmap) {
        for (Mining_Activity Activity_one : to_check) {
            for (Mining_Activity Activity_two : to_check) {
                if (relation_hashmap.get(Activity_one.getNode_ID()).get(Activity_two.getNode_ID()).getRelation_type() != Relation_Type.None
                        && Parallel_Hashmap.get(Activity_one.getNode_ID()).get(Activity_two.getNode_ID()).getRelation_type() != Relation_Type.None) {
                    return false;
                }
            }
        }
        return true;
    }

    //HELPER For MAX_SET
    private void Clean_Up_Place_List(List<Relation_Places> place_list) {
        ListIterator<Relation_Places> Place_Iterator_One = place_list.listIterator();
        List<Relation_Places> Mark_For_Deletion = new ArrayList<>();
        while (Place_Iterator_One.hasNext()) {
            Relation_Places place_one = Place_Iterator_One.next();
            ListIterator<Relation_Places> Place_Iterator_Two = place_list.listIterator();
            if (!Mark_For_Deletion.contains(place_one)) {
                while (Place_Iterator_Two.hasNext()) {
                    Relation_Places place_two = Place_Iterator_Two.next();
                    if (!Mark_For_Deletion.contains(place_two) && !place_one.equals(place_two) && place_one.has_Same_From(place_two) && place_one.has_Same_to(place_two)) {
                        Mark_For_Deletion.add(place_two);
                    }
                }
            }
        }
        place_list.removeAll(Mark_For_Deletion);
    }

    //Optionals

    //Generates a Filtered List of Activities for each Case with only the Started Activities to Calculate the
    // points in time in which a Activity started. Each Single Activity is instantiated as a Mining_Activity Object
    //in a list with the Same Design as the Case-List.
    private void FilterByActivityWorkingTime() {

        List<List<Mining_Instance>> sorted_Instance_List = Reader.getSorted_Instance_List();
        List<List<Mining_Activity>> used_Activity_per_Instance = new ArrayList<>();
        for (List<Mining_Instance> single_Instance_List : sorted_Instance_List) {
            List<Mining_Activity> used_Activity_By_Single_Instance = new ArrayList<>();
            for (Mining_Instance single_Activity : single_Instance_List) {
                if (single_Activity.getActivity_Status().equals("Working") && single_Activity.getActivity().getType_of_Activity().equals("Function")) {
                    used_Activity_By_Single_Instance.add(single_Activity.getActivity());
                }
            }
            used_Activity_per_Instance.add(used_Activity_By_Single_Instance);
        }
        Used_Activity_Filtered_List = used_Activity_per_Instance;

    }

    private void FilterByActivity() {
        List<List<Mining_Instance>> sorted_Instance_List = Reader.getSorted_Instance_List();
        List<List<Mining_Activity>> used_Activity_per_Instance = new ArrayList<>();
        for (List<Mining_Instance> single_Instance_List : sorted_Instance_List) {
            List<Mining_Activity> used_Activity_By_Single_Instance = new ArrayList<>();
            for (Mining_Instance single_Activity : single_Instance_List) {
                if (single_Activity.getActivity_Status().equals("Working") && single_Activity.getActivity().getType_of_Activity().equals("Function")) {
                    used_Activity_By_Single_Instance.add(single_Activity.getActivity());
                }
            }
            used_Activity_per_Instance.add(used_Activity_By_Single_Instance);
        }
        Used_Activity_Filtered_List = used_Activity_per_Instance;

    }
}