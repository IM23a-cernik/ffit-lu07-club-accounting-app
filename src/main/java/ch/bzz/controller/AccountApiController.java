package ch.bzz.controller;

import ch.bzz.generated.api.AccountApi;
import ch.bzz.generated.model.Account;
import ch.bzz.generated.model.AccountUpdate;
import ch.bzz.generated.model.UpdateAccountsRequest;
import ch.bzz.model.Project;
import ch.bzz.repository.AccountRepository;
import ch.bzz.repository.ProjectRepository;
import ch.bzz.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class AccountApiController implements AccountApi {

    @Autowired
    private HttpServletRequest request;

    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public AccountApiController(AccountRepository accountRepository, ProjectRepository projectRepository, JwtUtil jwtUtil) {
        this.accountRepository = accountRepository;
        this.projectRepository = projectRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public ResponseEntity<List<Account>> getAccounts() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String projectName = jwtUtil.getProject(token);
            Project project = projectRepository.findByProjectName(projectName);
            if (project == null) {
                log.error("Project not found");
                return ResponseEntity.status(404).build();
            }

            List<ch.bzz.model.Account> dbAccounts = accountRepository.findByProject(project);
            if (dbAccounts.isEmpty()) {
                log.warn("Account not found");
                Account dummyAccount = new Account();
                dummyAccount.setNumber(1100);
                dummyAccount.setName("FLL");
                return ResponseEntity.ok(List.of(dummyAccount));
            }

            List<Account> apiAccounts = dbAccounts.stream().map(db -> {
                Account account = new Account();
                account.setName(db.getName());
                account.setNumber(db.getAccountNumber());
                return account;
            }).toList();
            return ResponseEntity.ok(apiAccounts);
        }
        log.error("Authorization header not found");
        return ResponseEntity.status(401).build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> updateAccounts(UpdateAccountsRequest updateAccountsRequest) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String projectName = jwtUtil.getProject(token);
            Project project = projectRepository.findByProjectName(projectName);
            if (project == null) {
                log.error("Project not found");
                return ResponseEntity.status(404).build();
            }

            List<ch.bzz.model.Account> dbAccounts = accountRepository.findByProject(project);
            List<AccountUpdate> updateAccounts = updateAccountsRequest.getAccounts();

            for (AccountUpdate updateAccount : updateAccounts) {
                ch.bzz.model.Account match = dbAccounts.stream()
                        .filter(account -> account.getAccountNumber() == updateAccount.getNumber())
                        .findFirst().orElse(null);
                if (updateAccount.getName().orElse(null) == null) {
                    if (match != null) {
                        accountRepository.delete(match);
                        log.info("Account deleted: " + match.getAccountNumber());
                    }
                    continue;
                }

                if (match == null) {
                    ch.bzz.model.Account newAccount = new ch.bzz.model.Account();
                    newAccount.setAccountNumber(updateAccount.getNumber());
                    newAccount.setName(updateAccount.getName().orElse(null));
                    newAccount.setProject(project);
                    accountRepository.save(newAccount);
                    log.info("Account created");
                } else {
                    match.setAccountNumber(updateAccount.getNumber());
                    match.setName(updateAccount.getName().orElse(null));
                    accountRepository.save(match);
                    log.info("Account updated");
                }
            }

            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).build();
    }
}
