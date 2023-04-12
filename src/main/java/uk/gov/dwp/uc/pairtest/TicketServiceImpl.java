package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static uk.gov.dwp.uc.pairtest.enums.Type.*;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_ALLOWED_TICKETS = 20;
    private static final String MAX_ALLOWED_TICKETS_EXCEEDED_MESSAGE = "Sorry, unable to process request," +
            "it exceeds the maximum, 20 tickets is allowed at a time";
    private static final String INVALID_ACCOUNT_MESSAGE = "Sorry, unable to process your request as the provided" +
            " account is invalid";
    private static final String REQUIRED_AN_ADULT_MESSAGE = "Sorry, an Adult is required in other to complete your request";
    private static final String UNKNOWN_TICKET_TYPE = "Sorry, invalid ticket type requested";

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(final TicketPaymentService ticketPaymentService,
                             final SeatReservationService seatReservationService) {

        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        // validate the purchase request
        validatePurchaseRequest(accountId, ticketTypeRequests);

        // calculate total amount for requested ticket
        int totalTicketsPrice = calculateTotalAmount(ticketTypeRequests);

        // debit customer's account : Assume it will always be successful, no need to validate response.
        ticketPaymentService.makePayment(accountId, totalTicketsPrice);

        // calculate the total seats to allocate
        int totalSeatsToAllocate = calculateTotalSeatsToAllocate(ticketTypeRequests);

        // Reserve seat for the customer : Assume it will always be successful, no need to validate response.
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);

        System.out.println("Successfully purchased your tickets and reserved your seat(s)");
    }

    private void validatePurchaseRequest(final Long accountId, final TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {

        int numberOfAdults = 0;
        int numChildren = 0;
        int numberOfInfants = 0;

        //validate account to debit
        if (accountId <= 0)
            throw new InvalidPurchaseException(INVALID_ACCOUNT_MESSAGE);

        // Count the number of each ticket type
        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT -> numberOfAdults += request.getNoOfTickets();
                case CHILD -> numChildren += request.getNoOfTickets();
                case INFANT -> numberOfInfants += request.getNoOfTickets();
                default -> throw new InvalidPurchaseException(UNKNOWN_TICKET_TYPE);
            }
        }

        // confirm that at least one adult ticket is being purchased
        if (numberOfAdults == 0)
            throw new InvalidPurchaseException(REQUIRED_AN_ADULT_MESSAGE);

        // confirm that the total number of tickets does not exceed 20
        int totalTickets = numberOfAdults + numChildren + numberOfInfants;
        if (totalTickets > MAX_ALLOWED_TICKETS)
            throw new InvalidPurchaseException(MAX_ALLOWED_TICKETS_EXCEEDED_MESSAGE);

        /**NOTE:
         * Should we Confirm if more than one infants can sit on one adult lap?, if Yes, we need to ensure the
         * the number of infants is not more than the number of Adults
         * **/

//        if (numberOfInfants > numberOfAdults)
//            throw new InvalidPurchaseException("Number of Infant tickets can not be more than Adult tickets");
    }

    private int calculateTotalAmount(final TicketTypeRequest... ticketTypeRequests) {
        int totalAmount = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT -> totalAmount += request.getNoOfTickets() * ADULT.getPrice();
                case CHILD -> totalAmount += request.getNoOfTickets() * CHILD.getPrice();
            }
        }

        return totalAmount;
    }

    private int calculateTotalSeatsToAllocate(final TicketTypeRequest... ticketTypeRequests) {
        int totalSeatsToAllocate = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request.getTicketType() == ADULT || request.getTicketType() == CHILD) {
                totalSeatsToAllocate += request.getNoOfTickets();
            }
        }

        return totalSeatsToAllocate;
    }
}
