package com.ssafy.meari.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
public class CloudinaryConfig {

	@Bean
	public Cloudinary cloudinary() {
		return new Cloudinary(ObjectUtils.asMap(
			"cloud_name", "meari",
			"api_key", "254257143383711",
			"api_secret", "f3EWs0ckzl0pprPloOGgZTPCs_U"
		));
	}
}
