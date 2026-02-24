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

    public Map<String, Object> getCryptoByClientId(String clientId) {
        return clientProfileRepository.getCryptoByClientId(clientId);
    }

    

    public Map<String, Object> getEasebuzzMappingByCustomer(String custEmail, String custMobile) {
        return clientProfileRepository.findEasebuzzMappingByCustomer(custEmail, custMobile);
    }
    // ...existing code...
public Map<String, Object> getNttMappingByCustomer(String email, String mobile) {
    return clientProfileRepository.findNttMappingByCustomer(email, mobile);
}
// ...existing code...

}
