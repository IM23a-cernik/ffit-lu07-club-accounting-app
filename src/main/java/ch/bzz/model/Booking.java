package ch.bzz.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Booking {
    private int id;
    private Date date;
    private String text;
    private String debitAccount;
    private String creditAccount;
    private double amount;
    private String project;
}
