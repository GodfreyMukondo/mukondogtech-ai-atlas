package com.godfrey.ai_immigration_document_analyzer.dto.response;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**

 * Standard pagination response wrapper.
 * Used for paginated API endpoints.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**

     * List of records in current page.
     */
    private List<T> content;

    /**

     * Current page number (0-based).
     */
    private int page;

    /**

     * Size of each page.
     */
    private int size;

    /**

     * Total number of elements.
     */
    private long totalElements;

    /**

     * Total number of pages.
     */
    private int totalPages;

    /**

     * Indicates if this is last page.
     */
    private boolean last;

    /**

     * Indicates if this is first page.
     */
    private boolean first;

    /**

     * Creates a PagedResponse from Spring Page object.
     */
    public static <T> PagedResponse<T> from(org.springframework.data.domain.Page<T> pageData) {
        return PagedResponse.<T>builder()
                .content(pageData.getContent())
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .first(pageData.isFirst())
                .last(pageData.isLast())
                .build();
    }
}
