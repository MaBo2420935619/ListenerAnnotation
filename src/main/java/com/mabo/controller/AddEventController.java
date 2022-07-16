package com.mabo.controller;

import com.mabo.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("addEvent")
public class AddEventController {
    @Autowired
    private Event event;

    /**
     * @Author mabo
     * @Description   该方法主动调用
     * 浏览器输入下方地址即可测试
     *          http://localhost:8099/addEvent/add
     */
    @RequestMapping("add")
    public String add(){
        String test = event.event("测试");
        return test;
    }
    /**
     * @Author mabo
     * @Description   该方法主动调用
     * 浏览器输入下方地址即可测试
     *          http://localhost:8099/addEvent/reduce
     */
    @RequestMapping("reduce")
    public void reduce(){
        event.reduce("reduce测试");
    }
}
