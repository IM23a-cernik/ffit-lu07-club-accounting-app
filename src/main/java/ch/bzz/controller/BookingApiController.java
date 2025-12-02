package ch.bzz.controller;

import ch.bzz.generated.api.BookingApi;
import ch.bzz.generated.model.Booking;
import ch.bzz.generated.model.BookingUpdate;
import ch.bzz.generated.model.UpdateBookingsRequest;
import ch.bzz.model.Project;
import ch.bzz.model.Account;
import ch.bzz.repository.AccountRepository;
import ch.bzz.repository.BookingRepository;
import ch.bzz.repository.ProjectRepository;
import ch.bzz.util.JwtUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
public class BookingApiController implements BookingApi {
    @Autowired
    private HttpServletRequest request;

    private final BookingRepository bookingRepository;
    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final JwtUtil jwtUtil;
    private final EntityManager em;

    @Autowired
    public BookingApiController(BookingRepository bookingRepository,  ProjectRepository projectRepository, JwtUtil jwtUtil, EntityManager em, AccountRepository accountRepository) {
        this.bookingRepository = bookingRepository;
        this.accountRepository = accountRepository;
        this.projectRepository = projectRepository;
        this.jwtUtil = jwtUtil;
        this.em = em;
    }

    @Override
    public ResponseEntity<List<Booking>> getBookings() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String projectName = jwtUtil.getProject(token);
            Project project = em.getReference(Project.class, projectName);
            if (project == null) {
                log.error("Project not found");
                return ResponseEntity.status(404).build();
            }

            List<ch.bzz.model.Booking> dbBookings = bookingRepository.findByProject(project);
            if (dbBookings.isEmpty()) {
                log.warn("Booking not found");
                Booking dummyBooking = new Booking();
                dummyBooking.setNumber(1);
                dummyBooking.setDate(LocalDate.now());
                dummyBooking.setText("Dummy Booking");
                dummyBooking.setAmount(0.0F);
                dummyBooking.setDebit(1);
                dummyBooking.setCredit(1);
                return ResponseEntity.ok(List.of(dummyBooking));
            }
            List<Booking> apiBookings = dbBookings.stream().map(db -> {
                Booking booking = new Booking();
                booking.setNumber(db.getId());
                booking.setDate(db.getDate());
                booking.setText(db.getText());
                booking.setDebit(db.getDebitAccount().getAccountNumber());
                booking.setCredit(db.getCreditAccount().getAccountNumber());
                booking.setAmount(db.getAmount());
                return booking;
            }).toList();
            return ResponseEntity.ok(apiBookings);
        }
        log.error("Authorization header not found");
        return ResponseEntity.status(401).build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> updateBookings(UpdateBookingsRequest updateBookingsRequest) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String projectName = jwtUtil.getProject(token);
            Project project = projectRepository.findByProjectName(projectName);
            if (project == null) {
                log.error("Project not found");
                return ResponseEntity.status(404).build();
            }

            List<ch.bzz.model.Booking> dbBookings = bookingRepository.findByProject(project);
            List<BookingUpdate> updateBookings = updateBookingsRequest.getEntries();

            for (BookingUpdate updateBooking : updateBookings) {
                ch.bzz.model.Booking match = dbBookings.stream()
                        .filter(booking -> booking.getId() == updateBooking.getId())
                        .findFirst().orElse(null);
                Account creditAccount = accountRepository.findByAccountNumber(updateBooking.getCredit().orElse(null));
                Account debitAccount = accountRepository.findByAccountNumber(updateBooking.getDebit().orElse(null));

                if (match == null) {
                    ch.bzz.model.Booking newBooking = new ch.bzz.model.Booking();
                    newBooking.setDate(updateBooking.getDate().orElse(null));
                    newBooking.setText(updateBooking.getText().orElse(null));
                    newBooking.setAmount(updateBooking.getAmount().orElse(null));
                    newBooking.setProject(project);
                    newBooking.setCreditAccount(creditAccount);
                    newBooking.setDebitAccount(debitAccount);
                    newBooking.setId(updateBooking.getId());
                    bookingRepository.save(newBooking);
                    log.info("Account created");
                } else {
                    match.setAmount(updateBooking.getAmount().orElse(null));
                    match.setDate(updateBooking.getDate().orElse(null));
                    match.setText(updateBooking.getText().orElse(null));
                    match.setProject(project);
                    match.setDebitAccount(debitAccount);
                    match.setCreditAccount(creditAccount);
                    match.setId(updateBooking.getId());
                    bookingRepository.save(match);
                    log.info("Account updated");
                }
            }

            for (ch.bzz.model.Booking dbBooking : dbBookings) {
                boolean exists = updateBookings.stream()
                        .anyMatch(api -> api.getId() == dbBooking.getId());

                if (!exists) {
                    bookingRepository.delete(dbBooking);
                    log.info("Booking deleted: " + dbBooking.getId());
                }
            }

            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).build();
    }
}
