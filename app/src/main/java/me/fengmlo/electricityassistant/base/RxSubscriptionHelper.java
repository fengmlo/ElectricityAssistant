package me.fengmlo.electricityassistant.base;

import java.util.ArrayList;
import java.util.List;

import rx.Subscription;

public class RxSubscriptionHelper {

    private List<Subscription> subscriptionList = new ArrayList<>();

    public void addSubscribe(Subscription subscription) {
        subscriptionList.add(subscription);
    }

    public void unSubscribe() {
        for (Subscription subscription : subscriptionList) {
            if (!subscription.isUnsubscribed()) subscription.unsubscribe();
        }
    }
}
