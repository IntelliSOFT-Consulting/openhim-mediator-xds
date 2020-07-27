package org.openhim.mediator.dsub.service;

import akka.event.LoggingAdapter;
import org.openhim.mediator.dsub.pull.PullPoint;
import org.openhim.mediator.dsub.pull.PullPointFactory;
import org.openhim.mediator.dsub.subscription.MongoSubscriptionRepository;
import org.openhim.mediator.dsub.subscription.Subscription;
import org.openhim.mediator.dsub.subscription.SubscriptionNotifier;
import org.openhim.mediator.dsub.subscription.SubscriptionRepository;

import java.util.Date;
import java.util.List;
import java.net.URL;

public class DsubServiceImpl implements DsubService {

    private final PullPointFactory pullPointFactory;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionNotifier subscriptionNotifier;
    private final LoggingAdapter log;
    private final MongoSubscriptionRepository mongoSubscriptionRepository;

    public DsubServiceImpl(PullPointFactory pullPointFactory, SubscriptionRepository subscriptionRepository,
            SubscriptionNotifier subscriptionNotifier, LoggingAdapter log, MongoSubscriptionRepository mongoSubscriptionRepository) {
        this.pullPointFactory = pullPointFactory;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionNotifier = subscriptionNotifier;
        this.log = log;
        this.mongoSubscriptionRepository = mongoSubscriptionRepository;
    }

    public static boolean isUrlValid(String url) {

        try {
            new URL(url).toURI();
            return true;
        }

        catch (Exception e) {
            return false;
        }
    }

    @Override
    public void createSubscription(String url, String facilityQuery, Date terminateAt) {
        log.info("Request to create subscription for: " + url);

        if (isUrlValid(url)) {
            log.info("Valid URL: " + url);
                log.info("The URL Subscription doesn't exists or inactive: " + url);
                Subscription subscription = new Subscription(url, terminateAt, facilityQuery);

                subscriptionRepository.saveSubscription(subscription);
        }
        else{
            log.info("Invalid URL: " + url);
        }

    }

    @Override
    public void deleteSubscription(String url) {
        log.info("Request to delete subscription for: " + url);
        subscriptionRepository.deleteSubscription(url);
    }

    @Override
    public void notifyNewDocument(String docId, String facilityId) {
        List<Subscription> subscriptions = subscriptionRepository.findActiveSubscriptions(facilityId);

        log.info("Active subscriptions: {}", subscriptions.size());
        for (Subscription sub : subscriptions) {
            log.info("URL: {}", sub.getUrl());
            subscriptionNotifier.notifySubscription(sub, docId);
        }
    }

    @Override
    public void newDocumentForPullPoint(String docId, String locationId) {
        PullPoint pullPoint = pullPointFactory.get(locationId);
        pullPoint.registerDocument(docId);
    }

    @Override
    public List<String> getDocumentsForPullPoint(String locationId) {
        PullPoint pullPoint = pullPointFactory.get(locationId);
        return pullPoint.getDocumentIds();
    }
}
