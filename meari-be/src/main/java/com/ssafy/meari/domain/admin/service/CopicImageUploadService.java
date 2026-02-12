package com.ssafy.meari.domain.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CopicImageUploadService {

	private final Cloudinary cloudinary;

	public String uploadKopicPicture(byte[] fileBytes, String originalFilename) throws Exception {
		// 확장자 제거
		String publicId = originalFilename.replaceAll("\\.[^.]+$", "");

		Map result = cloudinary.uploader().upload(
			fileBytes,
			ObjectUtils.asMap(
				"use_filename", true,
				"unique_filename", false,
				"overwrite", true,
				"folder", "kopic",
				"public_id", publicId,
				"disable_cdn_cache", false
			)
		);

		return buildCloudinaryUrl((String) result.get("public_id"));
	}

	private String buildCloudinaryUrl(String publicId) {
		// publicId가 이미 kopic/로 시작하면 그대로, 아니면 kopic/ 추가
		String path = publicId.startsWith("kopic/") ? publicId : "kopic/" + publicId;
		return "https://res.cloudinary.com/meari/image/upload/" + path;
	}

	public List<String> uploadMultiKopicPictures(MultipartFile zipFile) throws Exception {

		List<String> urls = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {

			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) {

				if (entry.isDirectory())
					continue;

				String entryName = entry.getName();
				String publicId = entryName.replaceAll("\\.[^.]+$", "");

				byte[] bytes = zis.readAllBytes();

				Map result = cloudinary.uploader().upload(
					bytes,
					ObjectUtils.asMap(
						"use_filename", true,
						"unique_filename", true,
						"overwrite", false,
						"folder", "kopic",
						"public_id", publicId
					)
				);

				urls.add(buildCloudinaryUrl((String) result.get("public_id")));
			}
		}
		return urls;
	}
}
