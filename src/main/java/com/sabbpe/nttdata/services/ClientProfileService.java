package com.sabbpe.nttdata.services;

import com.sabbpe.nttdata.repositories.ClientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClientProfileService {

    private final ClientProfileRepository clientProfileRepository;

    public Map<String, Object> getKeys(String userId, String merchantId) {
        return clientProfileRepository.getKeys(userId, merchantId);
    }

}
