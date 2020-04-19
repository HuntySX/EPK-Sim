package com.company.EPK;

import com.company.Enums.Contype;
import com.company.Enums.Split_Decide_Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Event_Con_Split extends Connector {

    private Split_Decide_Type Decide_Type;
    private boolean is_Event_Driven;

    public Event_Con_Split(List<EPK_Node> Next_Elem, int ID, Contype contype, Split_Decide_Type decide_type) {
        super(Next_Elem, ID, contype);
        this.Decide_Type = decide_type;

    }

    public List<EPK_Node> choose_Next() {

        List<EPK_Node> Next_Elem = this.getNext_Elem();
        List<EPK_Node> Result = new ArrayList<>();
        if (Decide_Type == Split_Decide_Type.NORMAL) {
            //TODO Gaussian
        } else if (Decide_Type == Split_Decide_Type.SINGLE_RANDOM) {
            if (!Next_Elem.isEmpty()) {
                int count_Elem = Next_Elem.size();
                Random rand = new Random();
                count_Elem = rand.nextInt(count_Elem);
                Result.add(Next_Elem.get(count_Elem));
            }
        } else if (Decide_Type == Split_Decide_Type.FULL_RANDOM) {
            if (!Next_Elem.isEmpty()) {
                int count_Elem_Quantity = Next_Elem.size();
                Random rand = new Random();
                count_Elem_Quantity = rand.nextInt(count_Elem_Quantity);
                if (count_Elem_Quantity == 0) {
                    count_Elem_Quantity++;
                }

                List<EPK_Node> working_On_EPK_Nodes = getNext_Elem();
                for (int i = count_Elem_Quantity; i > 0; i--) {
                    int count_Elem = working_On_EPK_Nodes.size();
                    count_Elem = rand.nextInt(count_Elem);
                    Result.add(working_On_EPK_Nodes.get(count_Elem));
                    working_On_EPK_Nodes.remove(i);
                }
            }
        } else if (Decide_Type == Split_Decide_Type.EXPONENTIAL) {
            //TODO EXPONENTIAL
        } else if (Decide_Type == Split_Decide_Type.FULL) {
            //TODO FULL (AND)
        }
        return Result;
    }
}
