/*
    Graaljs shim for Response class returned from browser Fetch so we can use fetch() in Graaljs the same as in browser code

    https://developer.mozilla.org/en-US/docs/Web/API/Response
*/
global.Response = class {
    constructor (fetchOptions,response) {
        this.fetchOptions = fetchOptions;
        this.response = response;
    }
    get ok(){return this.response !== null;}
    set ok(v){throw new Error("read only");}

    get headers(){return this.response.headers || {}}

    get redirected(){return false;}

    get status(){return this.response.status}

    get statusText(){return this.response.statusText}

    get type(){}

    get url(){return this.fetchOptions.url}

    get useFinalURL(){}

    /* returns ReadableStream */
    get body(){return this.response.body}

    get bodyUsed(){}


    clone (){return new Response(this.fetchOptions,this.response);}
    error(){return new Response(this.fetchOptions,this.response);}
    redirect(url,status){/*TODO*/}
    arrayBuffer(){/*TODO*/}
    blob(){/*TODO*/}
    formData(){/*TODO*/}
    json(){
        const rtrn = new Promise((resolve,reject)=>{
            //TODO check the headers to ensure json content-type
            resolve(this.response.body);
        });
        return rtrn;
    }
    text(){
        const rtrn = new Promise((resolve,reject)=>{
            //TODO check the headers to ensure json content-type
            resolve(this.response.body);
        });
        return rtrn;
    }
}
