package ch.bzz.controller;

import ch.bzz.generated.api.AccountApi;
import ch.bzz.generated.model.Account;
import ch.bzz.generated.model.UpdateAccountsRequest;
import ch.bzz.model.Project;
import ch.bzz.repository.AccountRepository;
import ch.bzz.repository.ProjectRepository;
import ch.bzz.util.JwtUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class AccountApiController implements AccountApi {

    @Autowired
    private HttpServletRequest request;

    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final JwtUtil jwtUtil;
    private final EntityManager em;

    @Autowired
    public AccountApiController(AccountRepository accountRepository, ProjectRepository projectRepository, JwtUtil jwtUtil, EntityManager em) {
        this.accountRepository = accountRepository;
        this.projectRepository = projectRepository;
        this.jwtUtil = jwtUtil;
        this.em = em;
    }

    @Override
    public ResponseEntity<List<Account>> getAccounts() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String projectName = jwtUtil.getProject(token);
            Project project = em.getReference(Project.class, projectName);
            if (project == null) {
                return ResponseEntity.status(404).build();
            }

            List<ch.bzz.model.Account> dbAccounts = accountRepository.findByProject(project);
            if (dbAccounts.isEmpty()) {
                return ResponseEntity.status(404).build();
            }

            List<Account> apiAccounts = dbAccounts.stream().map(db -> {
                Account account = new Account();
                account.setName(db.getName());
                account.setNumber(db.getAccountNumber());
                return account;
            }).toList();
            return ResponseEntity.ok(apiAccounts);
        }
        return ResponseEntity.status(401).build();
    }

    @Override
    public ResponseEntity<Void> updateAccounts(UpdateAccountsRequest updateAccountsRequest) {
        return null;
    }
}
