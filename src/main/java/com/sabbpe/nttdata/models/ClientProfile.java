package com.sabbpe.nttdata.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_profile")
public class ClientProfile {

    @Id
    @Column(name = "client_id")
    private String clientId;  // PK only
}
