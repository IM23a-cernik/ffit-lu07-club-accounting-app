package ch.bzz.controller;

import ch.bzz.generated.api.BookingApi;
import ch.bzz.generated.model.Account;
import ch.bzz.generated.model.Booking;
import ch.bzz.generated.model.UpdateBookingsRequest;
import ch.bzz.model.Project;
import ch.bzz.repository.BookingRepository;
import ch.bzz.repository.ProjectRepository;
import ch.bzz.util.JwtUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class BookingApiController implements BookingApi {
    @Autowired
    private HttpServletRequest request;

    private final BookingRepository bookingRepository;
    private final ProjectRepository projectRepository;
    private final JwtUtil jwtUtil;
    private final EntityManager em;

    @Autowired
    public BookingApiController(BookingRepository bookingRepository,  ProjectRepository projectRepository, JwtUtil jwtUtil, EntityManager em) {
        this.bookingRepository = bookingRepository;
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
                return ResponseEntity.status(404).build();
            }

            List<ch.bzz.model.Booking> dbBookings = bookingRepository.findByProject(project);
            if (dbBookings.isEmpty()) {
                return ResponseEntity.status(404).build();
            }
            List<Booking> apiBookings = dbBookings.stream().map(db -> {
                Booking booking = new Booking();
                booking.setDate(db.getDate());
                booking.setText(db.getText());
                booking.setDebit(db.getDebitAccount().getId());
                booking.setCredit(db.getCreditAccount().getId());
                booking.setAmount(db.getAmount());
                return booking;
            }).toList();
            return ResponseEntity.ok(apiBookings);
        }
        return ResponseEntity.status(401).build();
    }

    @Override
    public ResponseEntity<Void> updateBookings(UpdateBookingsRequest updateBookingsRequest) {
        return null;
    }
}
