package com.mabo.listener;

import com.mabo.utils.AddEventListener;
import com.mabo.utils.EventType;
import org.springframework.stereotype.Component;

@Component
public class EventListener {
    @AddEventListener(value = "eventId",position = EventType.AFTEREVENT)
    public void test1(String s) {
        System.out.println("test1执行成功，参数:"+s);
    }

    @AddEventListener(value = "reduce")
    public void test2(String s) {
        System.out.println("test2执行成功，参数:"+s);
    }
}
