/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.openhim.mediator.dummies;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.openhim.mediator.datatypes.Identifier;
import org.openhim.mediator.engine.messages.MediatorRequestMessage;
import org.openhim.mediator.messages.BaseResolveIdentifier;
import org.openhim.mediator.messages.BaseResolveIdentifierResponse;
import org.openhim.mediator.messages.ResolvePatientIdentifier;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DummyResolveIdentifierActor extends UntypedActor {
    public static class ExpectedRequest {
        private Identifier identifier;
        private int seen = 0;

        public ExpectedRequest(Identifier identifier) {
            this.identifier = identifier;
        }

        public boolean wasSeen() {
            return seen>0;
        }

        public int getSeen() {
            return seen;
        }

        public Identifier getIdentifier() {
            return identifier;
        }
    }

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private ExpectedRequest expectedRequest;
    private List<ExpectedRequest> expectedRequests;
    private Class<BaseResolveIdentifier> expectedMessageClass;
    private Class<BaseResolveIdentifierResponse> responseClass;
    private Identifier responseIdentifier;

    public DummyResolveIdentifierActor(Class expectedMessageClass, Class<BaseResolveIdentifierResponse> responseClass, Identifier responseIdentifier, ExpectedRequest expectedRequest) {
        this(expectedMessageClass, responseClass, responseIdentifier);
        this.expectedRequest = expectedRequest;
    }

    public DummyResolveIdentifierActor(Class expectedMessageClass, Class<BaseResolveIdentifierResponse> responseClass, Identifier responseIdentifier, List<ExpectedRequest> expectedRequests) {
        this(expectedMessageClass, responseClass, responseIdentifier);
        this.expectedRequests = expectedRequests;
    }

    public DummyResolveIdentifierActor(Class expectedMessageClass, Class<BaseResolveIdentifierResponse> responseClass, Identifier responseIdentifier) {
        this.expectedMessageClass = expectedMessageClass;
        this.responseClass = responseClass;
        this.responseIdentifier = responseIdentifier;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (expectedMessageClass.isInstance(msg)) {
            if (expectedRequest!=null) {
                assertEquals(expectedRequest, msg);
                expectedRequest.seen++;
            } else if (expectedRequests!=null) {
                for (ExpectedRequest er : expectedRequests) {
                    if (er.identifier.equals(expectedMessageClass.cast(msg).getIdentifier())) {
                        er.seen++;
                    }
                }
            }

            log.info(
                    String.format(
                            "Received request to resolve '%s' in the '%s' domain",
                            expectedMessageClass.cast(msg).getIdentifier(),
                            expectedMessageClass.cast(msg).getTargetAssigningAuthority().getAssigningAuthorityId()
                    )
            );

            Object response = responseClass.getConstructor(MediatorRequestMessage.class, Identifier.class).newInstance(msg, responseIdentifier);
            getSender().tell(response, getSelf());
        } else {
            fail("Unexpected message received: " + msg.getClass());
        }
    }
}
