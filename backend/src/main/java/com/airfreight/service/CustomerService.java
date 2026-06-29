package com.airfreight.service;

import com.airfreight.entity.Customer;
import com.airfreight.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 客户服务 - 管理国内/国外客户信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer createCustomer(Customer customer) {
        Customer saved = customerRepository.save(customer);
        log.info("客户创建成功: {} ({})", saved.getName(), saved.getType());
        return saved;
    }

    public Customer updateCustomer(Long id, Customer updated) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("客户不存在: " + id));
        customer.setName(updated.getName());
        customer.setContactPerson(updated.getContactPerson());
        customer.setPhone(updated.getPhone());
        customer.setEmail(updated.getEmail());
        customer.setAddress(updated.getAddress());
        customer.setCountry(updated.getCountry());
        customer.setTaxId(updated.getTaxId());
        customer.setBankAccount(updated.getBankAccount());
        customer.setBankName(updated.getBankName());
        customer.setSwiftCode(updated.getSwiftCode());
        customer.setRemark(updated.getRemark());
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("客户不存在: " + id));
    }
}