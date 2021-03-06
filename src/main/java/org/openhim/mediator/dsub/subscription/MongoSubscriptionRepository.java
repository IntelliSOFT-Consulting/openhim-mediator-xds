package org.openhim.mediator.dsub.subscription;

import akka.event.LoggingAdapter;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.openhim.mediator.dsub.MongoSupport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MongoSubscriptionRepository extends MongoSupport implements SubscriptionRepository {

    private static final String URL = "url";
    private static final String FACILITY_QUERY = "facilityQuery";
    private static final String TERMINATE_AT = "terminateAt";

    private final LoggingAdapter log;

    public MongoSubscriptionRepository(MongoDatabase mongoDb, LoggingAdapter log) {
        super(mongoDb, "subscriptions");
        this.log = log;
    }

    @Override
    public void saveSubscription(Subscription subscription) {
        MongoCollection<Document> collection = getCollection();

        if (subscription.getUuid() == null) {
            subscription.setUuid(UUID.randomUUID().toString());
        }

        Document existing = collection.find(Filters.eq(ID,
                subscription.getUuid())).first();
        if (existing == null) {
            log.info("Saving subscription for: " + subscription.getUrl());

            Document doc = new Document(ID, subscription.getUuid())
                    .append(URL, subscription.getUrl())
                    .append(TERMINATE_AT, subscription.getTerminateAt())
                    .append(FACILITY_QUERY, subscription.getFacilityQuery());
            collection.insertOne(doc);
        } else {
            log.warning("Subscription already exists: " + subscription.getUuid());
        }
    }

    @Override
    public void deleteSubscription(String uuid) {
        DeleteResult result = getCollection().deleteMany(Filters.eq(ID, uuid));
        log.info("Deleted " + result.getDeletedCount() + " subscriptions for: " + uuid);
    }

    @Override
    public List<Subscription> findActiveSubscriptions(String facility) {
        FindIterable<Document> result = getCollection().find(
                Filters.and(
                    Filters.or(
                        Filters.eq(FACILITY_QUERY, facility),
                        Filters.eq(FACILITY_QUERY, null)
                    ),
                    Filters.or(
                            Filters.gt(TERMINATE_AT, new Date()),
                            Filters.eq(TERMINATE_AT, null)
                    )
                )
        );

        List<Subscription> subscriptions = new ArrayList<>();
        for (Document document : result) {
            subscriptions.add(deserialize(document));
        }

        return subscriptions;
    }

    private Subscription deserialize(Document document) {
        String uuid = document.getString(ID);
        String url =document.getString(URL);
        Date terminateAt = document.getDate(TERMINATE_AT);
        String facilityQuery = document.getString(FACILITY_QUERY);

        return new Subscription(url, terminateAt, facilityQuery, uuid);
    }
}
