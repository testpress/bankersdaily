package in.bankersdaily.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * References
 * https://gist.github.com/rahulgautam/25c72ffcac70dacb87bd#file-errorhandlingexecutorcalladapterfactory-java
 * https://github.com/square/retrofit/tree/master/samples/src/main/java/com/example/retrofit
 */
public class RetrofitCallAdapter<T> implements RetrofitCall<T> {
    private final Call<T> call;
    private final Executor callbackExecutor;

    RetrofitCallAdapter(Call<T> call, Executor callbackExecutor) {
        this.call = call;
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public void cancel() {
        call.cancel();
    }

    @Override
    public RetrofitCall<T> enqueue(final RetrofitCallback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull final Call<T> call, @NonNull final Response<T> response) {
                if (response.isSuccessful()) {
                    callbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(response.body());
                        }
                    });
                } else {
                    callbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onException(RetrofitException.httpError(response));
                        }
                    });
                }
            }

            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            @Override
            public void onFailure(@NonNull final Call<T> call, @NonNull final Throwable throwable) {
                RetrofitException exception;
                if (throwable instanceof IOException) {
                    exception = RetrofitException.networkError((IOException) throwable);
                } else {
                    exception = RetrofitException.unexpectedError(throwable);
                    exception.printStackTrace();
                }
                final RetrofitException finalException = exception;
                callbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onException(finalException);
                    }
                });
            }
        });
        return this;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public RetrofitCall<T> clone() {
        return new RetrofitCallAdapter<T>(call.clone(), callbackExecutor);
    }

    @Override
    public Response<T> execute() throws IOException {
        return call.execute();
    }
}
