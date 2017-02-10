/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.commonwl.viewer.services;

import org.apache.commons.io.IOUtils;
import org.commonwl.viewer.domain.GithubDetails;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles Github related functionality including API usage
 */
@Service
public class GitHubService {

    // Github API services
    private final ContentsService contentsService;
    private final UserService userService;

    // URL validation for directory links
    private final String GITHUB_DIR_REGEX = "^https:\\/\\/github\\.com\\/([A-Za-z0-9_.-]+)\\/([A-Za-z0-9_.-]+)\\/?(?:tree\\/([^/]+)\\/(.*))?$";
    private final Pattern githubDirPattern = Pattern.compile(GITHUB_DIR_REGEX);

    @Autowired
    public GitHubService(@Value("${githubAPI.authentication}") boolean authEnabled,
                         @Value("${githubAPI.username}") String username,
                         @Value("${githubAPI.password}") String password) {
        GitHubClient client = new GitHubClient();
        if (authEnabled) {
            client.setCredentials(username, password);
        }
        this.contentsService = new ContentsService(client);
        this.userService = new UserService(client);
    }

    /**
     * Extract the details of a Github directory URL using a regular expression
     * @param url The Github directory URL
     * @return A list with the groups of the regex match, [owner, repo, branch, path]
     */
    public GithubDetails detailsFromDirURL(String url) {
        Matcher m = githubDirPattern.matcher(url);
        if (m.find()) {
            return new GithubDetails(m.group(1), m.group(2), m.group(3), m.group(4));
        }
        return null;
    }

    /**
     * Get contents of a Github path from the API
     * @param githubInfo The information to access the repository
     * @return A list of details for the file(s) or false if there is an API error
     * @throws IOException Any API errors which may have occured
     */
    public List<RepositoryContents> getContents(GithubDetails githubInfo) throws IOException {
        return contentsService.getContents(new RepositoryId(githubInfo.getOwner(), githubInfo.getRepoName()),
                githubInfo.getPath(), githubInfo.getBranch());
    }

    /**
     * Get the details of a user from the Github API
     * @param username The username of the user we want to get data about
     * @return A user object containing the API information
     * @throws IOException Any API errors which may have occured
     */
    public User getUser(String username) throws IOException {
        return userService.getUser(username);
    }

    /**
     * Download a single file from a Github repository
     * @param githubInfo The information to access the repository
     * @return A string with the contents of the file
     * @throws IOException Any API errors which may have occured
     */
    public String downloadFile(GithubDetails githubInfo) throws IOException {
        // Download the file and return the contents
        // rawgit.com used to download individual files from git with the correct media type
        String url = String.format("https://cdn.rawgit.com/%s/%s/%s/%s", githubInfo.getOwner(),
                githubInfo.getRepoName(), githubInfo.getBranch(), githubInfo.getPath());
        URL downloadURL = new URL(url);
        InputStream download = downloadURL.openStream();
        try {
            return IOUtils.toString(download);
        } finally {
            IOUtils.closeQuietly(download);
        }

    }
}