/**
 * SPDX-FileCopyrightText: Copyright (c) 2015 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.spdx.tools.compare;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.Arrays;

import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.Annotation;
import org.spdx.library.model.v2.Checksum;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.ModelObjectV2;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxElement;
import org.spdx.library.model.v2.enumerations.FileType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.referencetype.ListedReferenceTypes;

/**
 * Helper class for comparisons
 * @author Gary O'Neall
 */
public class CompareHelper {

	static final int MAX_CHARACTERS_PER_CELL = 32000;
	private static final ConcurrentMap<Checksum, String> checksumCache = new ConcurrentHashMap<>();
	private static final ConcurrentMap<Annotation, String> annotationCache = new ConcurrentHashMap<>();
	private static final ConcurrentMap<Relationship, String> relationshipCache = new ConcurrentHashMap<>();

	/**
	 *
	 */
	private CompareHelper() {
		// Static helper, should not be instantiated
	}

	/**
	 * @param annotation
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String annotationToString(Annotation annotation) throws InvalidSPDXAnalysisException {
		return annotationCache.computeIfAbsent(annotation, ann -> {
			try {
				if (annotation == null) {
					return "";
				}
				StringBuilder sb = new StringBuilder(annotation.getAnnotationDate());
				sb.append(" ");
				sb.append(annotation.getAnnotator());
				sb.append(": ");
				sb.append(annotation.getComment());
				sb.append("[");
				sb.append(annotation.getAnnotationType().toString());
				sb.append("]");
				return sb.toString();
			} catch (InvalidSPDXAnalysisException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Create a string from an array of checksums
	 * @param checksums
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String checksumsToString(Collection<Checksum> checksums) throws InvalidSPDXAnalysisException {
		if (checksums == null || checksums.size() == 0) {
			return "";
		}
		List<String> cksumString = checksums.parallelStream()
				.map(cksum -> {
					try {
						return CompareHelper.checksumToString(cksum);
					} catch (InvalidSPDXAnalysisException e) {
						throw new RuntimeException(e);
					}
				})
				.sorted()
				.collect(Collectors.toList());
		StringBuilder sb = new StringBuilder(cksumString.get(0));
		for (int i = 1; i < cksumString.size(); i++) {
			sb.append("\n");
			if (sb.length() + cksumString.get(i).length() > MAX_CHARACTERS_PER_CELL) {
				int numRemaing = cksumString.size() - i;
				sb.append('[');
				sb.append(numRemaing);
				sb.append(" more...]");
				break;
			}
			sb.append(cksumString.get(i));
		}
		return sb.toString();
	}

	/**
	 * @param checksum
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String checksumToString(Checksum checksum) throws InvalidSPDXAnalysisException {
		return checksumCache.computeIfAbsent(checksum, cksum -> {
			try {
				if (checksum == null) {
					return "";
				}
				StringBuilder sb = new StringBuilder(checksum.getAlgorithm().toString());
				sb.append(' ');
				sb.append(checksum.getValue());
				return sb.toString();
			} catch (InvalidSPDXAnalysisException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * @param licenseInfos
	 * @return
	 */
	public static String licenseInfosToString(Collection<AnyLicenseInfo> licenseInfos) {
		if (licenseInfos == null || licenseInfos.size() == 0) {
			return "";
		}
		return licenseInfos.parallelStream()
				.map(licenseInfo -> {
						return licenseInfo.toString();
				})
				.collect(Collectors.joining(", "));
	}

	/**
	 * @param annotations
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String annotationsToString(Collection<Annotation> annotations) throws InvalidSPDXAnalysisException {
		if (annotations == null || annotations.size() == 0) {
			return "";
		}
		return annotations.parallelStream()
				.map(ann -> {
					try {
						return CompareHelper.annotationToString(ann);
					} catch (InvalidSPDXAnalysisException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.joining("\n"));
	}

	public static String attributionsToString(Collection<String> attributions) {
		if (attributions == null || attributions.size() == 0) {
			return "";
		}
		return attributions.parallelStream()
				.collect(Collectors.joining("\n"));
	}

	public static String relationshipToString(Relationship relationship) throws InvalidSPDXAnalysisException {
		return relationshipCache.computeIfAbsent(relationship, rel -> {
			try {
				if (relationship == null) {
					return "";
				}
				if (relationship.getRelationshipType() == null) {
					return "Unknown relationship type";
				}
				StringBuilder sb = new StringBuilder(relationship.getRelationshipType().toString());
				sb.append(":");
				Optional<SpdxElement> relatedElement = relationship.getRelatedSpdxElement();
				if (!relatedElement.isPresent()) {
					sb.append("?NULL");
				} else {
					Optional<String> relatedElementName = relatedElement.get().getName();
					if (relatedElementName.isPresent()) {
						sb.append('[');
						sb.append(relatedElementName.get());
						sb.append(']');
					}
					sb.append(relatedElement.get().getId());
				}
				Optional<String> comment = relationship.getComment();
				if (comment.isPresent() && !comment.get().isEmpty()) {
					sb.append('(');
					sb.append(comment.get());
					sb.append(')');
				}
				return sb.toString();
			} catch (InvalidSPDXAnalysisException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * @param relationships
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	public static String relationshipsToString(Collection<Relationship> relationships) throws InvalidSPDXAnalysisException {
		if (relationships == null || relationships.size() == 0) {
			return "";
		}
		return relationships.parallelStream()
				.map(rel -> {
					try {
						return CompareHelper.relationshipToString(rel);
					} catch (InvalidSPDXAnalysisException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.joining("\n"));
	}

	public static String formatSpdxElementList(Collection<SpdxElement> elements) throws InvalidSPDXAnalysisException {
		if (elements == null || elements.size() == 0) {
			return "";
		}
		return elements.parallelStream()
				.map(element -> {
					try {
						return CompareHelper.formatElement(element);
					} catch (InvalidSPDXAnalysisException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.joining(", "));
	}

	private static String formatElement(SpdxElement element) throws InvalidSPDXAnalysisException {
		if (Objects.isNull(element) || element.getId() == null || element.getId().isEmpty()) {
			return "[UNKNOWNID]";
		} else {
			StringBuilder sb = new StringBuilder(element.getId());
			Optional<String> name = element.getName();
			if (name.isPresent()) {
				sb.append('(');
				sb.append(name.get());
				sb.append(')');
			}
			return sb.toString();
		}
	}

	/**
	 * @param fileTypes
	 * @return
	 */
	public static String fileTypesToString(FileType[] fileTypes) {
		if (fileTypes == null || fileTypes.length == 0) {
			return "";
		}
		return Arrays.stream(fileTypes)
				.map(FileType::toString)
				.collect(Collectors.joining(", "));
	}

	/**
	 * Convert external refs to a friendly string
	 * @param externalRefs
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	public static String externalRefsToString(Collection<ExternalRef> externalRefs, String docNamespace) throws InvalidSPDXAnalysisException {
		if (externalRefs == null || externalRefs.size() == 0) {
			return "";
		}
		return externalRefs.parallelStream()
				.map(ref -> {
					try {
						return CompareHelper.externalRefToString(ref, docNamespace);
					} catch (InvalidSPDXAnalysisException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.joining("; "));
	}

	/**
	 * Convert a single external ref to a friendly string
	 * @param externalRef
	 * @param docNamespace
	 * @return
	 * @throws InvalidSPDXAnalysisException
	 */
	public static String externalRefToString(ExternalRef externalRef, String docNamespace) throws InvalidSPDXAnalysisException {
		String category = null;
		if (externalRef.getReferenceCategory() == null) {
			category = "OTHER";
		} else {
			category = externalRef.getReferenceCategory().toString();
		}
		String referenceType = null;
		if (externalRef.getReferenceType() == null) {
			referenceType = "[MISSING]";
		} else {
			try {
				referenceType = ListedReferenceTypes.getListedReferenceTypes().getListedReferenceName(new URI(externalRef.getReferenceType().getIndividualURI()));
			} catch (InvalidSPDXAnalysisException e) {
				referenceType = null;
			} catch (URISyntaxException e) {
				referenceType = null;
			}
			if (referenceType == null) {
				referenceType = externalRef.getReferenceType().getIndividualURI();
				if (docNamespace != null && !docNamespace.isEmpty() && referenceType.startsWith(docNamespace)) {
					referenceType = referenceType.substring(docNamespace.length());
				}
			}
		}
		String referenceLocator = externalRef.getReferenceLocator();
		if (referenceLocator == null) {
			referenceLocator = "[MISSING]";
		}
		String retval = category + " " + referenceType + " " + referenceLocator;
		Optional<String> comment = externalRef.getComment();
		if (comment.isPresent() && !comment.get().isEmpty()) {
			retval = retval + "(" + comment.get() + ")";
		}
		return retval;
	}

	public static String checksumToString(Optional<Checksum> checksum) throws InvalidSPDXAnalysisException {
		if (checksum.isPresent()) {
			return checksumToString(checksum.get());
		} else {
			return "[NONE]";
		}
	}

	public static boolean equivalent(Optional<? extends ModelObjectV2> c1, Optional<? extends ModelObjectV2> c2) throws InvalidSPDXAnalysisException {
		if (!c1.isPresent()) {
			return !c2.isPresent();
		}
		if (c2.isPresent()) {
			return (c1.get().equivalent(c2.get()));
		} else {
			return false;
		}
	}

	public static boolean equivalent(Collection<? extends ModelObjectV2> collection1, Collection<? extends ModelObjectV2> collection2) throws InvalidSPDXAnalysisException {
		if (Objects.isNull(collection1)) {
			return Objects.isNull(collection2);
		}
		if (Objects.isNull(collection2)) {
			return false;
		}
		if (collection1.size() != collection2.size()) {
			return false;
		}
		for (ModelObjectV2 o1:collection1) {
			boolean found = false;
			for (ModelObjectV2 o2:collection2) {
				if (o1.equivalent(o2)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}
}
