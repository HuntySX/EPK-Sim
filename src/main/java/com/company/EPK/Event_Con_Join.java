package com.company.EPK;

import com.company.Enums.Contype;
import com.company.Enums.Gate_Check_Status;
import com.company.Simulation.Simulation_Base.Data.Discrete_Data.Event_Instance;
import com.company.Simulation.Simulation_Base.Data.Discrete_Data.Instance_Workflow;
import com.company.Simulation.Simulation_Threading.Event_Gate;

import java.util.ArrayList;
import java.util.List;

import static com.company.Enums.Contype.*;
import static com.company.Enums.Gate_Check_Status.*;

public class Event_Con_Join extends Connector {

    private List<Nodemap> Mapped_Branch_Elements;
    private List<Event_Instance> Throughput_Instances;


    public Event_Con_Join(List<Node> Next_Elem, int ID, Contype contype) {
        super(Next_Elem, ID, contype);
        Throughput_Instances = new ArrayList<>();
    }

    public Gate_Check_Status check_Previous_Elem(Instance_Workflow instance) {

        List<Node> Finished_Nodes = instance.getInstance().getFinished_Work();
        List<Node> Scheduled_Nodes = instance.getInstance().getScheduled_Work();

        boolean Check_Single_Predecessor = false;
        boolean Check_Multiple_Predecessor = true;
        List<Node> Predecessors = new ArrayList<>();

        if (this.getContype() == EAGER_XOR) {

            if (!Scheduled_Nodes.isEmpty()) {
                return WAIT;
                //TODO Check Scheduled Notes for Functions, to Exclude Race Conditioning Gates
            } else {

                for (Nodemap m : Mapped_Branch_Elements) {
                    if (Finished_Nodes.contains(m.getFinished_Elem())) {
                        if (Check_Single_Predecessor) {
                            Check_Multiple_Predecessor = true;
                            break;
                        }
                        Check_Single_Predecessor = true;
                    }
                }
                if (Check_Multiple_Predecessor) {
                    return BLOCK;
                } else if (Check_Single_Predecessor) {
                    return ADVANCE;
                } else
                    return WAIT;
            }
        } else if (this.getContype() == LAZY_XOR) {

            if (Throughput_Instances.contains(instance.getInstance())) {
                return BLOCK;
            }
            Check_Single_Predecessor = false;
            Check_Multiple_Predecessor = false;
            Finished_Nodes = instance.getInstance().getFinished_Work();


            for (Nodemap m : Mapped_Branch_Elements) {
                Predecessors.add(m.getFinished_Elem());
            }

            for (Node n : Finished_Nodes) {
                for (Node m : Predecessors) {
                    if (n.getID() == m.getID()) {
                        if (Check_Single_Predecessor) {
                            Check_Multiple_Predecessor = true;
                            break;
                        }
                        Check_Single_Predecessor = true;
                    }
                }
                if (Check_Single_Predecessor || Check_Multiple_Predecessor) {
                    break;
                }
            }
            if (Check_Multiple_Predecessor) {
                return BLOCK;
            } else if (Check_Single_Predecessor) {
                Throughput_Instances.add(instance.getInstance());
                return ADVANCE;
            } else {
                return WAIT;
            }
        } else if (this.getContype() == EAGER_OR) {
            Finished_Nodes = instance.getInstance().getFinished_Work();
            Scheduled_Nodes = instance.getInstance().getScheduled_Work();
            List<Nodemap> Finishing_Map = new ArrayList<>();
            if (!Scheduled_Nodes.isEmpty()) {
                return WAIT;
                //TODO Check Scheduled Notes for Functions, to Exclude Race Conditioning Gates
            } else {


                for (Nodemap m : Mapped_Branch_Elements) {
                    if (Finished_Nodes.contains(m.getStarted_Elem()) || Scheduled_Nodes.contains(m.getStarted_Elem())) {
                        Finishing_Map.add(m);
                    }
                }
                for (Nodemap m : Finishing_Map) {
                    if (!Finished_Nodes.contains(m.getFinished_Elem())) {
                        return WAIT;
                    }
                }
                return ADVANCE;
            }
        } else if (this.getContype() == LAZY_OR) {

            if (Throughput_Instances.contains(instance.getInstance())) {
                return BLOCK;
            }

            Check_Single_Predecessor = false;

            for (Nodemap m : Mapped_Branch_Elements) {
                Predecessors.add(m.getFinished_Elem());
            }

            for (Node n : Finished_Nodes) {
                for (Node m : Predecessors) {
                    if (n.getID() == m.getID()) {
                        Check_Single_Predecessor = true;
                        break;
                    }
                }
                if (Check_Single_Predecessor) {
                    break;
                }
            }
            if (Check_Single_Predecessor) {
                Throughput_Instances.add(instance.getInstance());
                return ADVANCE;
            } else {
                return WAIT;
            }

        } else {
            for (Nodemap m : Mapped_Branch_Elements) {
                if (!Finished_Nodes.contains(m)) {
                    return WAIT;
                }
            }
            return ADVANCE;
        }

    }
}