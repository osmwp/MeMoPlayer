/*
 * Copyright (C) 2010 France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * BML Filter for Apache
 * Original version by: Thomas MILLET
 * See README for compilation and installation
 *
 */

/*
 * Include the core server components.
 */
#include "httpd.h"
#include "http_config.h"
#include "http_log.h"
#include "apr_strings.h"
#include "apr_general.h"
#include "util_filter.h"
#include "apr_buckets.h"
#include "http_request.h"
#include "http_protocol.h"
#include "apr_lib.h"
#include "bml.h"


/**
 * Name of the filter for conf directives
 */
static const char bmlFilterName[] = "BML";

/**
 * Mime type for BML
 */
static const char bmlMimeType[] = "application/bml";

/**
 * Contexte to save XML Data
 */
struct bml_ctx{
    char *xmlData;
    apr_size_t dataLen;
};

/**
 * Main BML filter processing
 */
static apr_status_t bml_out_filter(ap_filter_t *f, apr_bucket_brigade *bb)
{
    //Buffer for IN XML Data
    struct bml_ctx *bmlCtx = (struct bml_ctx *)f->ctx;

    //Buffer for OUT XML Data
    char *bmlData = NULL;
    int bmlLen;

    //Buffer for TMP Data
    char *tmpData = NULL;
    apr_size_t tmpLen;

    //Output Brigade
    apr_bucket_brigade *pbbOut;

    //APR Status
    apr_status_t status;

    //Protection of process in case of empty packets
    if (APR_BRIGADE_EMPTY(bb)) {
        return APR_SUCCESS;
    }

    apr_bucket *pkbbPtr = APR_BRIGADE_FIRST(bb);

    //Read input brigade and store content
    while (pkbbPtr != APR_BRIGADE_SENTINEL(bb)) {
        //EOS of Brigade
        if(!APR_BUCKET_IS_EOS(pkbbPtr)){

            //Read current bucket
            status  = apr_bucket_read(pkbbPtr, (const char**)&tmpData, &tmpLen, APR_BLOCK_READ);
            if (status == APR_SUCCESS) {
                //First Allocation and copy
                if(bmlCtx == NULL){
                    bmlCtx = (struct bml_ctx*)malloc(sizeof *bmlCtx);
                    bmlCtx->xmlData = (char*)malloc(sizeof(char)*tmpLen);
                    memcpy(bmlCtx->xmlData,tmpData,tmpLen);
                    bmlCtx->dataLen = tmpLen;
                    f->ctx = bmlCtx;
                }
                //Reallocation and concat
                else{
                    bmlCtx->xmlData = (char*) realloc(bmlCtx->xmlData,sizeof(char)*(tmpLen+bmlCtx->dataLen));
                    memcpy(bmlCtx->xmlData+bmlCtx->dataLen,tmpData,tmpLen);
                    bmlCtx->dataLen += tmpLen;
                }
                APR_BUCKET_REMOVE(pkbbPtr);
            }
            else return status;
        }
        else{
            // Purge bucket brigade for reuse
            apr_brigade_cleanup(bb);

            //Encode xml data as bml
            Encoder encoder (bmlCtx->xmlData, &bmlData, bmlLen);
            free(bmlCtx);

            apr_bucket *e;
            if (bmlLen <= 0) {
                // On failure, return a 500 error.
                //ap_log_rerror(APLOG_MARK, APLOG_ERR, 0, f->r, "BML CONVERSION FAILED !");
                f->r->status_line = "500 Internal Server Error : BML conversion failed";
                e = ap_bucket_error_create(HTTP_INTERNAL_SERVER_ERROR, NULL, f->r->pool, f->c->bucket_alloc);
                APR_BRIGADE_INSERT_TAIL(bb, e);
                e = apr_bucket_eos_create(f->c->bucket_alloc);
                APR_BRIGADE_INSERT_TAIL(bb, e);
                ap_pass_brigade(f->next, bb);
                return AP_FILTER_ERROR;
            }
            // Insert bucket with bml data
            e = apr_bucket_heap_create(bmlData, bmlLen, free,  f->c->bucket_alloc);
            APR_BRIGADE_INSERT_TAIL(bb,e);
            // Add content lenght / type info
            ap_set_content_length(f->r, bmlLen);
            ap_set_content_type(f->r, bmlMimeType);
            // Add EOS and pass brigade to next filter
            e = apr_bucket_eos_create(f->c->bucket_alloc);
            APR_BRIGADE_INSERT_TAIL(bb,e);
            return ap_pass_brigade(f->next, bb);
        }

        pkbbPtr = APR_BUCKET_NEXT(pkbbPtr);
    }

    return APR_SUCCESS;

}

/*
 * This function is a callback and it declares what other functions
 * should be called for request processing and configuration requests.
 * This callback function declares the Handlers for other events.
 */
static void mod_bml_register_hooks (apr_pool_t *p)
{
    ap_register_output_filter(bmlFilterName, bml_out_filter, NULL,
                                  AP_FTYPE_CONTENT_SET);
}

/*
 * Declare and populate the module's data structure.  The
 * name of this structure ('bml_module') is important - it
 * must match the name of the module.  This structure is the
 * only "glue" between the httpd core and the module.
 */
module AP_MODULE_DECLARE_DATA bml_module =
{
    STANDARD20_MODULE_STUFF, // standard stuff; no need to mess with this.
    NULL, // create per-directory configuration structures - we do not.
    NULL, // merge per-directory - no need to merge if we are not creating anything.
    NULL, // create per-server configuration structures.
    NULL, // merge per-server - hrm - examples I have been reading don't bother with this for trivial cases.
    NULL, // configuration directive handlers
    mod_bml_register_hooks // request handlers
};
