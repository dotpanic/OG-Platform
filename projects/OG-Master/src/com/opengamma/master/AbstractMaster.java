/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master;

import com.opengamma.DataNotFoundException;
import com.opengamma.id.UniqueIdentifier;

/**
 * An outline definition of a general-purpose master storing documents.
 * <p>
 * The master provides a uniform view over a set of documents.
 * This interface provides methods that allow the documents to be
 * added, updated, corrected, removed and retrieved.
 * Sub interfaces typically add methods for searching.
 * 
 * @param <D>  the type of the document
 */
public interface AbstractMaster<D extends AbstractDocument> {

  /**
   * Gets a document by unique identifier.
   * <p>
   * If the master supports history then the version in the identifier will be used
   * to return the requested historic version.
   * 
   * @param uid  the unique identifier, not null
   * @return the document, not null
   * @throws IllegalArgumentException if the request is invalid
   * @throws DataNotFoundException if there is no document with that unique identifier
   */
  D get(UniqueIdentifier uid);

  /**
   * Adds a document to the data store.
   * <p>
   * The version instant, correction instant and identifier will be set in the response.
   * 
   * @param document  the document, not null
   * @return the added document, may be an update of the input document, not null
   * @throws IllegalArgumentException if the request is invalid
   */
  D add(D document);

  /**
   * Updates a document in the data store.
   * <p>
   * The specified document must contain the unique identifier.
   * If the identifier has a version it must be the latest version.
   * The version instant, correction instant and identifier will be set in the response.
   * <p>
   * A full master will store detailed historic information on documents.
   * Thus, an update does not prevent retrieval or correction of an earlier version.
   * 
   * @param document  the document, not null
   * @return the current state of the document, may be an update of the input document, not null
   * @throws IllegalArgumentException if the request is invalid
   * @throws DataNotFoundException if there is no document with that unique identifier
   */
  D update(D document);

  /**
   * Removes a document from the data store.
   * <p>
   * A full master will store detailed historic information on documents.
   * Thus, a removal does not prevent retrieval or correction of an earlier version.
   * <p>
   * If the identifier has a version it must be the latest version.
   * 
   * @param uid  the unique identifier to remove, not null
   * @throws IllegalArgumentException if the request is invalid
   * @throws DataNotFoundException if there is no document with that unique identifier
   */
  void remove(final UniqueIdentifier uid);

  /**
   * Corrects a document in the data store.
   * <p>
   * A full master will store detailed historic information on documents
   * and will support correction of each version.
   * To update a document with a new version, use {@link #update}.
   * To correct a previously stored version, use this method.
   * Older versions and corrections can be accessed using a versioned identifier.
   * <p>
   * The specified document must contain the unique identifier.
   * The unique identifier must specify the last correction of a specific version of the document.
   * 
   * @param document  the document, not null
   * @return the corrected state of the version, may be an update of the input document, not null
   * @throws IllegalArgumentException if the request is invalid
   * @throws DataNotFoundException if there is no document with that unique identifier
   */
  D correct(D document);

}
