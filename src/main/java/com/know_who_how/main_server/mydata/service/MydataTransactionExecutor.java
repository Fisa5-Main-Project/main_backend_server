package com.know_who_how.main_server.mydata.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MydataTransactionExecutor {

    @Transactional
    public void execute(Runnable action) {
        action.run();
    }
}
