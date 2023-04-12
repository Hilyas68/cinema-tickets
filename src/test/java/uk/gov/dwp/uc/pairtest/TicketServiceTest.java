package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.enums.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    private static final String REQUIRED_AN_ADULT_MESSAGE = "Sorry, an Adult is required in other to complete your request";
    private static final String INVALID_ACCOUNT_MESSAGE = "Sorry, unable to process your request as the provided" +
            " account is invalid";
    private static final String MAX_ALLOWED_TICKETS_EXCEEDED_MESSAGE = "Sorry, unable to process request," +
            "it exceeds the maximum, 20 tickets is allowed at a time";

    @Mock
    private TicketPaymentService paymentService;

    @Mock
    private SeatReservationService seatService;

    private TicketService ticketService;

    @BeforeEach
    public void init() {

        ticketService = new TicketServiceImpl(paymentService, seatService);
    }

    @Test
    public void purchaseTicket_withValidRequest_returnSuccess() {

        // GIVEN
        final TicketTypeRequest adultTicket = new TicketTypeRequest(Type.ADULT, 2);
        final TicketTypeRequest childTicket = new TicketTypeRequest(Type.CHILD, 1);
        final TicketTypeRequest infantTicket = new TicketTypeRequest(Type.INFANT, 1);

        final long accountId = 1L;
        final int expectedPaymentAmount = 50;
        final int expectedSeatToAllocate = 3;

        // WHEN
        ticketService.purchaseTickets(accountId, adultTicket, childTicket, infantTicket);

        // THEN
        verify(paymentService, times(1)).makePayment(eq(accountId), eq(expectedPaymentAmount));
        verify(seatService, times(1)).reserveSeat(eq(accountId), eq(expectedSeatToAllocate));
    }

    @Test
    public void purchaseTicket_withInValidAccountId_returnInvalidPurchaseException() {

        // GIVEN
        final TicketTypeRequest adultTicket = new TicketTypeRequest(Type.ADULT, 2);
        final TicketTypeRequest childTicket = new TicketTypeRequest(Type.CHILD, 1);

        final long accountId = 0L;

        final InvalidPurchaseException invalidPurchaseException =
                Assertions.assertThrows(InvalidPurchaseException.class, () ->
                        ticketService.purchaseTickets(accountId, childTicket, adultTicket));

        assertEquals(invalidPurchaseException.getMessage(), INVALID_ACCOUNT_MESSAGE);

        verify(paymentService, never()).makePayment(anyLong(), anyInt());
        verify(seatService, never()).reserveSeat(anyLong(), anyInt());
    }

    @Test
    public void purchaseTicket_withMissingAdultTicket_returnInvalidPurchaseException() {

        // GIVEN
        final TicketTypeRequest childTicket = new TicketTypeRequest(Type.CHILD, 2);
        final TicketTypeRequest infantTicket = new TicketTypeRequest(Type.INFANT, 4);
        final long accountId = 1L;

        // WHEN
        final InvalidPurchaseException invalidPurchaseException =
                Assertions.assertThrows(InvalidPurchaseException.class, () ->
                        ticketService.purchaseTickets(accountId, childTicket, infantTicket));

        // THEN
        assertEquals(invalidPurchaseException.getMessage(), REQUIRED_AN_ADULT_MESSAGE);
        verify(paymentService, never()).makePayment(anyLong(), anyInt());
        verify(seatService, never()).reserveSeat(anyLong(), anyInt());
    }

    @Test
    public void purchaseTicket_withInvalidMaxTicketNumber_returnInvalidPurchaseException() {

        // GIVEN
        final TicketTypeRequest adultTicket = new TicketTypeRequest(Type.ADULT, 21);
        final long accountId = 1L;

        // WHEN
        final InvalidPurchaseException invalidPurchaseException =
                Assertions.assertThrows(InvalidPurchaseException.class, () ->
                        ticketService.purchaseTickets(accountId, adultTicket));

        // THEN
        assertEquals(invalidPurchaseException.getMessage(), MAX_ALLOWED_TICKETS_EXCEEDED_MESSAGE);
        verify(paymentService, never()).makePayment(anyLong(), anyInt());
        verify(seatService, never()).reserveSeat(anyLong(), anyInt());
    }

}
