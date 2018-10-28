package com.java.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.java.dto.Product;

public interface ProductRepository extends JpaRepository<Product, Integer>{
	
}
